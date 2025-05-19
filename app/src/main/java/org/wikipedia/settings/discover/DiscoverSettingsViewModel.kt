package org.wikipedia.settings.discover

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wikipedia.readinglist.recommended.RecommendedReadingListSource
import org.wikipedia.readinglist.recommended.RecommendedReadingListUpdateFrequency
import org.wikipedia.settings.Prefs

class DiscoverSettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DiscoverSettingsState())
    val uiState: StateFlow<DiscoverSettingsState> = _uiState.asStateFlow()

    fun toggleRecommendedReadingList(enabled: Boolean) {
        Prefs.isRecommendedReadingListEnabled = enabled
        _uiState.value = _uiState.value.copy(isRecommendedReadingListEnabled = enabled)
        if (!enabled) {
            Prefs.isRecommendedReadingListNotificationEnabled = false
        }
    }

    fun updateArticleNumberForRecommendingReadingList(number: Int) {
        Prefs.recommendedReadingListArticlesNumber = number
        _uiState.value = _uiState.value.copy(articlesNumber = number)
    }

    fun updateFrequency(frequency: RecommendedReadingListUpdateFrequency) {
        Prefs.recommendedReadingListUpdateFrequency = frequency
        _uiState.value = _uiState.value.copy(updateFrequency = frequency)
    }

    fun toggleNotification(enabled: Boolean) {
        Prefs.isRecommendedReadingListNotificationEnabled = enabled
        _uiState.value = _uiState.value.copy(isRecommendedReadingListNotificationEnabled = enabled)
    }
}

data class DiscoverSettingsState(
    val isRecommendedReadingListEnabled: Boolean = Prefs.isRecommendedReadingListEnabled,
    val articlesNumber: Int = Prefs.recommendedReadingListArticlesNumber,
    val updateFrequency: RecommendedReadingListUpdateFrequency = Prefs.recommendedReadingListUpdateFrequency,
    val recommendedReadingListSource: RecommendedReadingListSource = Prefs.recommendedReadingListSource,
    val isRecommendedReadingListNotificationEnabled: Boolean = Prefs.isRecommendedReadingListNotificationEnabled
)
