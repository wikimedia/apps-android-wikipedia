package org.wikipedia.activitytab

import android.text.format.DateUtils
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.categories.db.Category
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.games.onthisday.OnThisDayGameViewModel
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

class ActivityTabViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _readingHistoryState = MutableStateFlow<UiState<ReadingHistory>>(UiState.Loading)
    val readingHistoryState: StateFlow<UiState<ReadingHistory>> = _readingHistoryState.asStateFlow()

    private val _donationUiState = MutableStateFlow<UiState<String?>>(UiState.Loading)
    val donationUiState: StateFlow<UiState<String?>> = _donationUiState.asStateFlow()

    private val _wikiGamesUiState = MutableStateFlow<UiState<OnThisDayGameViewModel.GameStatistics?>>(UiState.Loading)
    val wikiGamesUiState: StateFlow<UiState<OnThisDayGameViewModel.GameStatistics?>> = _wikiGamesUiState.asStateFlow()

    private val repository = TimelineRepository(userName = AccountUtil.userName)
    private val pageSize = 50

    private val _timelineState = MutableStateFlow(TimelineUiState())
    val timelineState = _timelineState.asStateFlow()

    private val allTimelineItems = mutableListOf<TimelineItem>()

    fun loadAll() {
        loadReadingHistory()
        loadDonationResults()
        loadWikiGamesStats()
        loadInitialTimeline()
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

            val gamesStats = OnThisDayGameViewModel.getGameStatistics(WikipediaApp.instance.wikiSite.languageCode)
            _wikiGamesUiState.value = UiState.Success(gamesStats)
        }
    }

    fun createPageTitleForCategory(category: Category): PageTitle {
        return PageTitle(title = category.title, wiki = WikiSite.forLanguageCode(category.lang))
    }

    fun loadInitialTimeline() {
        viewModelScope.launch {
            _timelineState.value = TimelineUiState(isInitialLoading = true)
            loadTimelinePage(0, true)
        }
    }

    fun loadNextPage() {
        val currentState = _timelineState.value
        if (currentState.isLoadingMore || !currentState.hasMoreData) return
        viewModelScope.launch {
            _timelineState.value = currentState.copy(isLoadingMore = true, hasError = false)
            loadTimelinePage(currentState.currentPage + 1, isInitial = false)
        }
    }

    private suspend fun loadTimelinePage(page: Int, isInitial: Boolean) {
        try {
            val (mergedItems, hasMoreData) = repository.getTimelinePage(page, pageSize)
            if (isInitial) {
                allTimelineItems.clear()
                allTimelineItems.addAll(mergedItems)
            } else {
                // Append new items, avoiding duplicates
                val newItems = mergedItems.filter { newItem ->
                    allTimelineItems.none { it.id == newItem.id }
                }
                allTimelineItems.addAll(newItems)
            }

            _timelineState.value = _timelineState.value.copy(
                items = allTimelineItems.toList(),
                isLoadingMore = false,
                isInitialLoading = false,
                hasMoreData = hasMoreData,
                currentPage = page,
                hasError = false
            )
        } catch (e: Exception) {
            _timelineState.value = _timelineState.value.copy(
                isLoadingMore = false,
                isInitialLoading = false,
                hasError = true,
                errorMessage = e.message ?: "Unknown error occurred"
            )
        }
    }

    fun retryLoading() {
        if (_timelineState.value.items.isEmpty()) {
            loadInitialTimeline()
        } else {
            loadNextPage()
        }
    }

    fun refresh() {
        allTimelineItems.clear()
        loadInitialTimeline()
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
