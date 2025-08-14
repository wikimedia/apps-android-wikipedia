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
import org.wikipedia.util.UiState
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class ActivityTabViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _timeSpentState = MutableStateFlow<UiState<Long>>(UiState.Loading)
    val timeSpentState: StateFlow<UiState<Long>> = _timeSpentState.asStateFlow()

    var gameStatistics: OnThisDayGameViewModel.GameStatistics? = null
    var donationResults: List<DonationResult> = emptyList()

    var topCategories: List<Category> = emptyList()

    init {
        loadTimeSpent()
    }

    fun loadTimeSpent() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _timeSpentState.value = UiState.Error(throwable)
        }) {
            _timeSpentState.value = UiState.Loading
            val now = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val sevenDaysAgo = now - TimeUnit.DAYS.toMillis(7)
            val totalTimeSpent = AppDatabase.instance.historyEntryWithImageDao().getTimeSpentSinceTimeStamp(sevenDaysAgo)
            _timeSpentState.value = UiState.Success(totalTimeSpent)
        }
    }
}
