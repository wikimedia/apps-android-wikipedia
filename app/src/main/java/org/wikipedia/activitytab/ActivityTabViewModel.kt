package org.wikipedia.activitytab

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.categories.db.Category
import org.wikipedia.database.AppDatabase
import org.wikipedia.donate.DonationResult
import org.wikipedia.games.onthisday.OnThisDayGameViewModel
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.UiState
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class ActivityTabViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _readingHistoryState = MutableStateFlow<UiState<ReadingHistory>>(UiState.Loading)
    val readingHistoryState: StateFlow<UiState<ReadingHistory>> = _readingHistoryState.asStateFlow()

    var gameStatistics: OnThisDayGameViewModel.GameStatistics? = null
    var donationResults: List<DonationResult> = emptyList()

    var topCategories: List<Category> = emptyList()

    init {
        loadReadingHistory()
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
            val articlesReadThisMonth = AppDatabase.instance.historyEntryDao().getTotalEntriesSince(thirtyDaysAgo) ?: 0
            val articlesReadByWeek = mutableListOf<Int>()
            articlesReadByWeek.add(AppDatabase.instance.historyEntryDao().getTotalEntriesSince(weekAgo) ?: 0)
            for (i in 1..3) {
                weekAgo -= weekInMillis
                val articlesRead = AppDatabase.instance.historyEntryDao().getHistoryCount(weekAgo, weekAgo + weekInMillis)
                articlesReadByWeek.add(articlesRead)
            }
            val mostRecentReadTime = AppDatabase.instance.historyEntryDao().getMostRecentEntry()?.timestamp?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime()

            val articlesSavedThisMonth = AppDatabase.instance.readingListPageDao().getTotalPagesSince(thirtyDaysAgo) ?: 0
            val articlesSaved = AppDatabase.instance.readingListPageDao().getPagesSince(thirtyDaysAgo, 4)
                .map { ReadingListPage.toPageTitle(it) }
            val mostRecentSaveTime = AppDatabase.instance.readingListPageDao().getMostRecentSavedPage()?.mtime?.let { Instant.ofEpochMilli(it) }?.atZone(ZoneId.systemDefault())?.toLocalDateTime()

            _readingHistoryState.value = UiState.Success(ReadingHistory(
                timeSpentThisWeek = totalTimeSpent,
                articlesReadThisMonth = articlesReadThisMonth,
                lastArticleReadTime = mostRecentReadTime,
                articlesReadByWeek = articlesReadByWeek,
                articlesSavedThisMonth = articlesSavedThisMonth,
                lastArticleSavedTime = mostRecentSaveTime,
                articlesSaved = articlesSaved)
            )
        }
    }

    class ReadingHistory(
        val timeSpentThisWeek: Long,
        val articlesReadThisMonth: Int,
        val lastArticleReadTime: LocalDateTime?,
        val articlesReadByWeek: List<Int>,
        val articlesSavedThisMonth: Int,
        val lastArticleSavedTime: LocalDateTime?,
        val articlesSaved: List<PageTitle>
    )
}
