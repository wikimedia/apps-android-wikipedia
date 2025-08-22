package org.wikipedia.activitytab

import android.text.format.DateUtils
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.categories.db.Category
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.growthtasks.GrowthUserImpact
import org.wikipedia.games.onthisday.OnThisDayGameViewModel
import org.wikipedia.json.JsonUtil
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.settings.Prefs
import org.wikipedia.util.UiState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class ActivityTabViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    var langCode = Prefs.userContribFilterLangCode

    val wikiSite get(): WikiSite {
        return when (langCode) {
            Constants.WIKI_CODE_COMMONS -> WikiSite(Service.COMMONS_URL)
            Constants.WIKI_CODE_WIKIDATA -> WikiSite(Service.WIKIDATA_URL)
            else -> WikiSite.forLanguageCode(langCode)
        }
    }

    private val _readingHistoryState = MutableStateFlow<UiState<ReadingHistory>>(UiState.Loading)
    val readingHistoryState: StateFlow<UiState<ReadingHistory>> = _readingHistoryState.asStateFlow()

    private val _donationUiState = MutableStateFlow<UiState<String?>>(UiState.Loading)
    val donationUiState: StateFlow<UiState<String?>> = _donationUiState.asStateFlow()

    private val _wikiGamesUiState = MutableStateFlow<UiState<OnThisDayGameViewModel.GameStatistics?>>(UiState.Loading)
    val wikiGamesUiState: StateFlow<UiState<OnThisDayGameViewModel.GameStatistics?>> = _wikiGamesUiState.asStateFlow()

    private val historyEntrySource = HistoryEntrySource(AppDatabase.instance.historyEntryWithImageDao())
    private val apiSource = ApiTimelineSource(wikiSite, AccountUtil.userName)
    private val readingListSource = ReadingListSource(AppDatabase.instance.readingListPageDao())
    private var currentTimelinePagingSource: TimelinePagingSource? = null

    val timelineFlow = Pager(
        config = PagingConfig(pageSize = 50),
        pagingSourceFactory = { TimelinePagingSource(listOf(historyEntrySource, apiSource, readingListSource)).also {
            currentTimelinePagingSource = it
        } }
    ).flow.cachedIn(viewModelScope)
        .map { pagingData ->
            pagingData.insertSeparators { before, after ->
                if (before == null && after != null) TimelineDisplayItem.DateSeparator(after.timestamp)
                else if (before != null && after != null && before.timestamp.toLocalDate() != after.timestamp.toLocalDate()) {
                    TimelineDisplayItem.DateSeparator(after.timestamp)
                } else null
            }.map { item ->
                when (item) {
                    is TimelineItem -> TimelineDisplayItem.TimelineEntry(item)
                    else -> item as TimelineDisplayItem
                }
            }
        }

    private val _impactUiState = MutableStateFlow<UiState<GrowthUserImpact>>(UiState.Loading)
    val impactUiState: StateFlow<UiState<GrowthUserImpact>> = _impactUiState.asStateFlow()

    fun loadAll() {
        loadReadingHistory()
        loadDonationResults()
        loadWikiGamesStats()
        loadImpact()
    }

    fun refreshTimeline() {
        currentTimelinePagingSource?.invalidate()
    }

    fun loadReadingHistory() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _readingHistoryState.value = UiState.Error(throwable)
        }) {
            _readingHistoryState.value = UiState.Loading
            val now = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val weekInMillis = TimeUnit.DAYS.toMillis(7)
            var weekAgo = now - weekInMillis
            val totalTimeSpent = AppDatabase.instance.historyEntryWithImageDao().getTimeSpentSinceTimeStamp(weekAgo)

            val thirtyDaysAgo = now - TimeUnit.DAYS.toMillis(30)
            val articlesReadThisMonth = AppDatabase.instance.historyEntryDao().getDistinctEntriesSince(thirtyDaysAgo) ?: 0
            val articlesReadByWeek = mutableListOf<Int>()
            articlesReadByWeek.add(AppDatabase.instance.historyEntryDao().getDistinctEntriesSince(weekAgo) ?: 0)
            for (i in 1..3) {
                weekAgo -= weekInMillis
                val articlesRead = AppDatabase.instance.historyEntryDao().getDistinctEntriesBetween(weekAgo, weekAgo + weekInMillis)
                articlesReadByWeek.add(articlesRead)
            }
            val mostRecentReadTime = AppDatabase.instance.historyEntryDao().getMostRecentEntry()?.timestamp?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime()

            val articlesSavedThisMonth = AppDatabase.instance.readingListPageDao().getTotalPagesSince(thirtyDaysAgo) ?: 0
            val articlesSaved = AppDatabase.instance.readingListPageDao().getPagesSince(thirtyDaysAgo, 4)
                .map { ReadingListPage.toPageTitle(it) }
            val mostRecentSaveTime = AppDatabase.instance.readingListPageDao().getMostRecentSavedPage()?.mtime?.let { Instant.ofEpochMilli(it) }?.atZone(ZoneId.systemDefault())?.toLocalDateTime()

            val currentDate = LocalDate.now()
            val topCategories = AppDatabase.instance.categoryDao().getTopCategoriesByMonth(currentDate.year, currentDate.monthValue)

            _readingHistoryState.value = UiState.Success(ReadingHistory(
                timeSpentThisWeek = totalTimeSpent,
                articlesReadThisMonth = articlesReadThisMonth,
                lastArticleReadTime = mostRecentReadTime,
                articlesReadByWeek = articlesReadByWeek,
                articlesSavedThisMonth = articlesSavedThisMonth,
                lastArticleSavedTime = mostRecentSaveTime,
                articlesSaved = articlesSaved,
                topCategories.take(3))
            )
        }
    }

    fun loadDonationResults() {
        val lastDonationTime = Prefs.donationResults.lastOrNull()?.dateTime?.let {
            val timestampInLong = LocalDateTime.parse(it).toInstant(ZoneOffset.UTC).epochSecond
            val relativeTime = DateUtils.getRelativeTimeSpanString(
                timestampInLong * 1000, // Convert seconds to milliseconds
                System.currentTimeMillis(),
                0L
            )
            return@let relativeTime.toString()
        }
        _donationUiState.value = UiState.Success(lastDonationTime)
    }

    fun loadWikiGamesStats() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _wikiGamesUiState.value = UiState.Error(throwable)
        }) {
            _wikiGamesUiState.value = UiState.Loading
            val lastGameHistory = AppDatabase.instance.dailyGameHistoryDao().findLastGameHistory()
            if (lastGameHistory == null) {
                _wikiGamesUiState.value = UiState.Success(null)
                return@launch
            }

            val gamesStats =
                OnThisDayGameViewModel.getGameStatistics(WikipediaApp.instance.wikiSite.languageCode)
            _wikiGamesUiState.value = UiState.Success(gamesStats)
        }
    }

    fun loadImpact() {
        if (!AccountUtil.isLoggedIn) {
            return
        }
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _impactUiState.value = UiState.Error(throwable)
        }) {
            _impactUiState.value = UiState.Loading

            // The impact API is rate limited, so we cache it manually.
            val now = Instant.now().epochSecond
            val impact: GrowthUserImpact
            if (Prefs.impactLastResponseBody.isEmpty() || abs(now - Prefs.impactLastQueryTime) > TimeUnit.DAYS.toSeconds(1)) {
                Prefs.impactLastResponseBody = ""
                val userId = ServiceFactory.get(WikipediaApp.instance.wikiSite).getUserInfo().query?.userInfo?.id!!
                impact = ServiceFactory.getCoreRest(WikipediaApp.instance.wikiSite).getUserImpact(userId)
                Prefs.impactLastResponseBody = JsonUtil.encodeToString(impact).orEmpty()
                Prefs.impactLastQueryTime = now
            } else {
                impact = JsonUtil.decodeFromString(Prefs.impactLastResponseBody)!!
            }
            _impactUiState.value = UiState.Success(impact)
        }
    }

    fun createPageTitleForCategory(category: Category): PageTitle {
        return PageTitle(title = category.title, wiki = WikiSite.forLanguageCode(category.lang))
    }

    class ReadingHistory(
        val timeSpentThisWeek: Long,
        val articlesReadThisMonth: Int,
        val lastArticleReadTime: LocalDateTime?,
        val articlesReadByWeek: List<Int>,
        val articlesSavedThisMonth: Int,
        val lastArticleSavedTime: LocalDateTime?,
        val articlesSaved: List<PageTitle>,
        val topCategories: List<Category>
    )

    companion object {
        const val CAMPAIGN_ID = "appmenu_activity"
    }
}

sealed class TimelineDisplayItem {
    data class DateSeparator(val date: Date) : TimelineDisplayItem()
    data class TimelineEntry(val item: TimelineItem) : TimelineDisplayItem()
}
