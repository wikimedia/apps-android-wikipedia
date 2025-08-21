package org.wikipedia.activitytab

import android.text.format.DateUtils
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.categories.db.Category
import org.wikipedia.database.AppDatabase
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
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class ActivityTabViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _readingHistoryState = MutableStateFlow<UiState<ReadingHistory>>(UiState.Loading)
    val readingHistoryState: StateFlow<UiState<ReadingHistory>> = _readingHistoryState.asStateFlow()

    private val _donationUiState = MutableStateFlow<UiState<String?>>(UiState.Loading)
    val donationUiState: StateFlow<UiState<String?>> = _donationUiState.asStateFlow()

    private val _wikiGamesUiState = MutableStateFlow<UiState<OnThisDayGameViewModel.GameStatistics?>>(UiState.Loading)
    val wikiGamesUiState: StateFlow<UiState<OnThisDayGameViewModel.GameStatistics?>> = _wikiGamesUiState.asStateFlow()

    private val _impactUiState = MutableStateFlow<UiState<GrowthUserImpact>>(UiState.Loading)
    val impactUiState: StateFlow<UiState<GrowthUserImpact>> = _impactUiState.asStateFlow()

    fun loadAll() {
        loadReadingHistory()
        loadDonationResults()
        loadWikiGamesStats()
        loadImpact()
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

            // Use page/summary to get the proper page title.
            val summaryResponses = impact.topViewedArticles.map {
                async {
                    val pageSummaryResponse = ServiceFactory.getRest(WikipediaApp.instance.wikiSite).getPageSummary(it.key)
                    pageSummaryResponse to it.value
                }
            }

            summaryResponses.awaitAll().forEach {
                impact.topViewedArticlesWithPageSummary[it.first] = it.second
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
