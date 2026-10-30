package org.wikipedia.createaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.auth.AccountUtil
import org.wikipedia.database.AppDatabase
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.settings.Prefs
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class CreateAccountEncourageViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        Prefs.createAccountEncourageImpressions += 1
        Prefs.createAccountEncourageLastImpressionDate = LocalDate.now().toString()
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
                savedArticles = AppDatabase.instance.readingListPageDao().getDistinctPagesCount(excludedStatus = ReadingListPage.STATUS_QUEUE_FOR_DELETE),
                recentReads = AppDatabase.instance.historyEntryDao().getDistinctEntriesCountSince(thirtyDaysAgo) ?: 0
            )
        }
    }

    data class UiState(
        val readingDays: Int = 0,
        val savedArticles: Int = 0,
        val recentReads: Int = 0
    )

    companion object {
        private const val READING_SPAN_DAYS = 14
        private const val READING_SPAN_MIN_AGE_DAYS = 7L
        private const val READING_HISTORY_CUTOFF_DAYS = 365L
        private const val RETURN_DAYS_REQUIRED = 2

        suspend fun shouldShow(): Boolean {
            if (AccountUtil.isLoggedIn && !AccountUtil.isTemporaryAccount) {
                return false
            }
            return when (Prefs.createAccountEncourageImpressions) {
                -1 -> true // for testing and debugging
                0 -> hasReadOnTwoDaysWithinAFortnight()
                1 -> hasReturnedSinceLastImpression()
                else -> false
            }
        }

        private suspend fun hasReadOnTwoDaysWithinAFortnight(): Boolean {
            // The earlier of the two days must itself be a week old, otherwise the fortnight
            // containing both of them would have to extend into the future.
            val now = System.currentTimeMillis()
            return AppDatabase.instance.historyEntryDao().hasReadingDaysApartWithin(
                maxDaysApart = READING_SPAN_DAYS,
                firstDayOnOrBeforeMillis = now - TimeUnit.DAYS.toMillis(READING_SPAN_MIN_AGE_DAYS),
                sinceMillis = now - TimeUnit.DAYS.toMillis(READING_HISTORY_CUTOFF_DAYS)
            )
        }

        private suspend fun hasReturnedSinceLastImpression(): Boolean {
            val lastImpressionDate = runCatching { LocalDate.parse(Prefs.createAccountEncourageLastImpressionDate) }
                .getOrNull() ?: return false

            val sinceMillis = lastImpressionDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val historyEntryDao = AppDatabase.instance.historyEntryDao()
            return historyEntryDao.getDistinctReadingDaysCountSince(sinceMillis) >= RETURN_DAYS_REQUIRED
        }
    }
}
