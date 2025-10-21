package org.wikipedia.activitytab

import android.text.format.DateUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.activitytab.timeline.HistoryEntryPagingSource
import org.wikipedia.activitytab.timeline.ReadingListPagingSource
import org.wikipedia.activitytab.timeline.TimelineItem
import org.wikipedia.activitytab.timeline.TimelinePagingSource
import org.wikipedia.activitytab.timeline.TimelineSource
import org.wikipedia.activitytab.timeline.UserContribPagingSource
import org.wikipedia.auth.AccountUtil
import org.wikipedia.categories.db.Category
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.growthtasks.GrowthUserImpact
import org.wikipedia.extensions.toLocalDate
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

class ActivityTabViewModel() : ViewModel() {
    private val _readingHistoryState = MutableStateFlow<UiState<ReadingHistory>>(UiState.Loading)
    val readingHistoryState: StateFlow<UiState<ReadingHistory>> = _readingHistoryState.asStateFlow()

    private val _donationUiState = MutableStateFlow<UiState<String?>>(UiState.Loading)
    val donationUiState: StateFlow<UiState<String?>> = _donationUiState.asStateFlow()

    private val _wikiGamesUiState = MutableStateFlow<UiState<OnThisDayGameViewModel.GameStatistics?>>(UiState.Loading)
    val wikiGamesUiState: StateFlow<UiState<OnThisDayGameViewModel.GameStatistics?>> = _wikiGamesUiState.asStateFlow()

    private var currentTimelinePagingSource: TimelinePagingSource? = null

    val wikiSiteForTimeline get(): WikiSite {
        val langCode = Prefs.userContribFilterLangCode
        return when (langCode) {
            Constants.WIKI_CODE_COMMONS -> WikiSite(Service.COMMONS_URL)
            Constants.WIKI_CODE_WIKIDATA -> WikiSite(Service.WIKIDATA_URL)
            else -> WikiSite.forLanguageCode(langCode)
        }
    }

    val timelineFlow = Pager(
        config = PagingConfig(
            pageSize = 150,
            prefetchDistance = 20
        ),
        pagingSourceFactory = { TimelinePagingSource(
            createTimelineSources()
        ).also {
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
    private val _totalEditsUiState = MutableStateFlow<UiState<Int>>(UiState.Loading)
    val totalEditsUiState: StateFlow<UiState<Int>> = _totalEditsUiState.asStateFlow()

    var shouldRefreshTimelineSilently: Boolean = false

    val allDataLoaded = combine(
        readingHistoryState,
        donationUiState,
        wikiGamesUiState,
        impactUiState
    ) { reading, donation, games, impact ->
        reading !is UiState.Loading &&
                donation !is UiState.Loading &&
                games !is UiState.Loading &&
                impact !is UiState.Loading
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun loadAll() {
        loadReadingHistory()
        if (!AccountUtil.isLoggedIn) {
            return
        }
        loadDonationResults()
        loadWikiGamesStats()
        loadImpact()
        refreshTimeline()
    }

    private fun refreshTimeline() {
        currentTimelinePagingSource?.invalidate()
    }

    fun loadReadingHistory() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _readingHistoryState.value = UiState.Error(throwable)
        }) {
            _readingHistoryState.value = UiState.Loading
            delay(500)
            val now = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val weekInMillis = TimeUnit.DAYS.toMillis(7)
            var weekAgo = now - weekInMillis
            val totalTimeSpent = AppDatabase.instance.historyEntryWithImageDao().getTimeSpentBetween(weekAgo)

            val thirtyDaysAgo = now - TimeUnit.DAYS.toMillis(30)
            val articlesReadThisMonth = AppDatabase.instance.historyEntryDao().getDistinctEntriesCountSince(thirtyDaysAgo) ?: 0
            val articlesReadByWeek = mutableListOf<Int>()
            articlesReadByWeek.add(AppDatabase.instance.historyEntryDao().getDistinctEntriesCountSince(weekAgo) ?: 0)
            for (i in 1..3) {
                weekAgo -= weekInMillis
                val articlesRead = AppDatabase.instance.historyEntryDao().getDistinctEntriesCountBetween(weekAgo, weekAgo + weekInMillis)
                articlesReadByWeek.add(articlesRead)
            }
            val mostRecentReadTime = AppDatabase.instance.historyEntryDao().getMostRecentEntry()?.timestamp?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime()

            val articlesSavedThisMonth = AppDatabase.instance.readingListPageDao().getTotalLocallySavedPagesBetween(thirtyDaysAgo) ?: 0
            val articlesSaved = AppDatabase.instance.readingListPageDao().getLocallySavedPagesSince(thirtyDaysAgo, 4)
                .map { ReadingListPage.toPageTitle(it) }
            val mostRecentSaveTime = AppDatabase.instance.readingListPageDao().getMostRecentLocallySavedPage()?.atime?.let { Instant.ofEpochMilli(it) }?.atZone(ZoneId.systemDefault())?.toLocalDateTime()

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
            delay(500)
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
            val wikiSite = WikipediaApp.instance.wikiSite
            val now = Instant.now().epochSecond
            val impact: GrowthUserImpact
            val impactLastResponseBodyMap = Prefs.impactLastResponseBody.toMutableMap()
            val impactResponse = impactLastResponseBodyMap[wikiSite.languageCode]
            if (impactResponse.isNullOrEmpty() || abs(now - Prefs.impactLastQueryTime) > TimeUnit.HOURS.toSeconds(12)) {
                val userId = ServiceFactory.get(wikiSite).getUserInfo().query?.userInfo?.id!!
                impact = ServiceFactory.getCoreRest(wikiSite).getUserImpact(userId)
                impactLastResponseBodyMap[wikiSite.languageCode] = JsonUtil.encodeToString(impact).orEmpty()
                Prefs.impactLastResponseBody = impactLastResponseBodyMap
                Prefs.impactLastQueryTime = now
            } else {
                impact = JsonUtil.decodeFromString(impactResponse)!!
            }

            val pagesResponse = ServiceFactory.get(wikiSite).getInfoByTitlesWithGlobalUserInfo(
                titles = impact.topViewedArticles.keys.joinToString(separator = "|")
            )
            _totalEditsUiState.value = UiState.Success(pagesResponse.query?.globalUserInfo?.editCount ?: 0)

            // Transform the response to a map of PageTitle to ArticleViews
            val pageMap = pagesResponse.query?.pages?.associate { page ->
                val pageTitle = PageTitle(
                    text = page.title,
                    wiki = wikiSite,
                    thumbUrl = page.thumbUrl(),
                    description = page.description,
                    displayText = page.displayTitle(wikiSite.languageCode)
                )
                pageTitle to impact.topViewedArticles[pageTitle.text]!!
            } ?: emptyMap()

            impact.topViewedArticlesWithPageTitle = pageMap

            _impactUiState.value = UiState.Success(impact)
        }
    }

    fun createPageTitleForCategory(category: Category): PageTitle {
        return PageTitle(title = category.title, wiki = WikiSite.forLanguageCode(category.lang))
    }

    private fun createTimelineSources(): List<TimelineSource> {
        val historyEntryPagingSource = HistoryEntryPagingSource(AppDatabase.instance.historyEntryWithImageDao())
        val userContribPagingSource = UserContribPagingSource(wikiSiteForTimeline, AccountUtil.userName, AppDatabase.instance.historyEntryWithImageDao())
        val readingListPagingSource = ReadingListPagingSource(AppDatabase.instance.readingListPageDao())
        return listOf(historyEntryPagingSource, readingListPagingSource, userContribPagingSource)
    }

    fun getTotalEditsCount(): Int {
        return when (val currentState = _impactUiState.value) {
            is UiState.Success -> currentState.data.totalEditsCount
            else -> 0
        }
    }

    fun hasNoDonationData(): Boolean {
        return when (val currentState = _donationUiState.value) {
            is UiState.Success -> currentState.data == null
            else -> true
        }
    }

    fun hasNoReadingHistoryData(): Boolean {
        return when (val currentState = _readingHistoryState.value) {
            is UiState.Success -> {
                val data = currentState.data
                data.timeSpentThisWeek <= 0 && data.articlesReadThisMonth <= 0 && data.articlesSavedThisMonth <= 0 && data.topCategories.isEmpty()
            }
            else -> true
        }
    }

    fun hasNoImpactData(): Boolean {
        return when (val currentState = _impactUiState.value) {
            is UiState.Success -> {
                val data = currentState.data
                data.totalEditsCount <= 0 && data.receivedThanksCount <= 0 && data.totalPageviewsCount <= 0
            }
            else -> true
        }
    }

    fun hasNoGameStats(): Boolean {
        return when (val currentState = _wikiGamesUiState.value) {
            is UiState.Success -> {
                val data = currentState.data ?: return true
                data.totalGamesPlayed <= 0
            }
            else -> true
        }
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
