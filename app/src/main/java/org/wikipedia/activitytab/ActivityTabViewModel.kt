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
            val sevenDaysAgo = now - weekInMillis
            val totalTimeSpent = AppDatabase.instance.historyEntryWithImageDao().getTimeSpentSinceTimeStamp(sevenDaysAgo)

            val thirtyDaysAgo = now - TimeUnit.DAYS.toMillis(30)
            val articlesReadThisMonth = AppDatabase.instance.historyEntryDao().getTotalEntriesSince(thirtyDaysAgo) ?: 0
            val articlesReadByWeek = mutableListOf<Int>()
            for (i in 1..4) {
                val weekAgo = now - weekInMillis
                val articlesRead = AppDatabase.instance.historyEntryDao().getTotalEntriesBetween(weekAgo, weekAgo + weekInMillis)
                articlesReadByWeek.add(articlesRead?.toInt() ?: 0)
            }

            val articlesSavedThisMonth = AppDatabase.instance.readingListPageDao().getTotalPagesSince(thirtyDaysAgo) ?: 0
            val articlesSaved = AppDatabase.instance.readingListPageDao().getPagesSince(thirtyDaysAgo, 4)
                .map { ReadingListPage.toPageTitle(it) }

            _readingHistoryState.value = UiState.Success(ReadingHistory(
                totalTimeSpent,
                articlesReadThisMonth,
                articlesReadByWeek,
                articlesSavedThisMonth,
                articlesSaved)
            )
        }
    }

    class ReadingHistory(
        val timeSpentThisWeek: Long,
        val articlesReadThisMonth: Long,
        val articlesReadByWeek: List<Int>,
        val articlesSavedThisMonth: Long,
        val articlesSaved: List<PageTitle>
    )
}
