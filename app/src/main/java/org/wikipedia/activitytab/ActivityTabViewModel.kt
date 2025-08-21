package org.wikipedia.activitytab

import android.text.format.DateUtils
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.activitytab.timeline.TimelineRepository
import org.wikipedia.activitytab.timeline.TimelineState
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
import java.util.Date
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

    private val _timelineState = MutableStateFlow(TimelineState(isLoadingDatabase = true))
    val timelineState: StateFlow<TimelineState> = _timelineState.asStateFlow()

    private val repository = TimelineRepository(userName = AccountUtil.userName)
    private var currentApiContinueToken: String? = null

    val mergedTimelineItems = _timelineState
        .map { state ->
            // Only recomputes when database items or api items change
            val allApiItems = state.apiItemsByPage.values.flatten()
            (state.databaseItems + allApiItems)
                .sortedByDescending { it.timestamp }
                .distinctBy { it.id }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun loadAll() {
        loadReadingHistory()
        loadDonationResults()
        loadWikiGamesStats()
        loadImpact()
        loadTimeline()
    }

    fun loadTimeline() {
        loadDatabaseItems()
        loadNextApiPage()
    }

    private fun loadDatabaseItems() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _timelineState.value = _timelineState.value.copy(
                isLoadingDatabase = false,
                error = throwable
            )
        }) {
            _timelineState.value = _timelineState.value.copy(isLoadingDatabase = true)

            val databaseItems = repository.getLocalTimelineItems()

            _timelineState.value = _timelineState.value.copy(
                databaseItems = databaseItems,
                isLoadingDatabase = false,
                isLoadingApi = true
            )
        }
    }

    private fun loadNextApiPage() {
        if (!_timelineState.value.hasMoreApiData || _timelineState.value.isLoadingApi) return

        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _timelineState.value = _timelineState.value.copy(
                isLoadingApi = false,
                error = throwable
            )
        }) {
            _timelineState.value = _timelineState.value.copy(isLoadingApi = true)
            val apiResult = repository.getWikipediaContributions(20, currentApiContinueToken)
            val currentPage = _timelineState.value.apiPageCount

            _timelineState.value = _timelineState.value.copy(
                apiItemsByPage = _timelineState.value.apiItemsByPage + (currentPage to apiResult.items),
                apiPageCount = currentPage + 1,
                isLoadingApi = false,
                hasMoreApiData = apiResult.nextToken != null
            )

            currentApiContinueToken = apiResult.nextToken
        }
    }

    fun loadMoreApiItems() {
        loadNextApiPage()
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

// Extension functions
fun Date.toLocalDate(): LocalDate {
    return this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}

fun Date.isToday(): Boolean {
    return this.toLocalDate() == LocalDate.now()
}

fun Date.isYesterday(): Boolean {
    return this.toLocalDate() == LocalDate.now().minusDays(1)
}
