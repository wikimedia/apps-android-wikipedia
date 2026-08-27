package org.wikipedia.createaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.database.AppDatabase
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class CreateAccountEncourageViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadPersonalizedCounts()
    }

    private fun loadPersonalizedCounts() {
        viewModelScope.launch(CoroutineExceptionHandler { _, _ ->
            _uiState.value = UiState()
        }) {
            val startOfYear = LocalDate.now().withDayOfYear(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)

            _uiState.value = UiState(
                readingDays = AppDatabase.instance.historyEntryDao().getDistinctReadingDaysCountSince(startOfYear),
                savedArticles = AppDatabase.instance.readingListPageDao().getPagesCount(),
                recentReads = AppDatabase.instance.historyEntryDao().getDistinctEntriesCountSince(thirtyDaysAgo) ?: 0
            )
        }
    }

    data class UiState(
        val readingDays: Int = 0,
        val savedArticles: Int = 0,
        val recentReads: Int = 0
    )
}
