package org.wikipedia.readinglist.recommended

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.database.AppDatabase
import org.wikipedia.settings.Prefs
import org.wikipedia.util.Resource

class RecommendedReadingListSettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RecommendedReadingListSettingsState())
    val uiState: StateFlow<RecommendedReadingListSettingsState> = _uiState.asStateFlow()

    private val _resetUiState = MutableStateFlow(Resource<Boolean>())
    val resetUiState = _resetUiState.asStateFlow()

    fun toggleDiscoverReadingList(enabled: Boolean) {
        Prefs.isRecommendedReadingListEnabled = enabled
        if (enabled) {
            // Should reshow the onboarding if it was previously skipped
            Prefs.isRecommendedReadingListOnboardingShown = AppDatabase.instance.recommendedPageDao().findIfAny() == null
        }
        if (!enabled) {
            Prefs.isRecommendedReadingListNotificationEnabled = false
        }
        _uiState.value = _uiState.value.copy(isRecommendedReadingListEnabled = enabled, isRecommendedReadingListNotificationEnabled = Prefs.isRecommendedReadingListNotificationEnabled)
    }

    fun updateArticleNumbers(number: Int) {
        if (number != Prefs.recommendedReadingListArticlesNumber) {
            Prefs.resetRecommendedReadingList = true
        }
        Prefs.recommendedReadingListArticlesNumber = number
        _uiState.value = _uiState.value.copy(articlesNumber = number)
    }

    fun updateFrequency(frequency: RecommendedReadingListUpdateFrequency) {
        if (frequency != Prefs.recommendedReadingListUpdateFrequency) {
            Prefs.resetRecommendedReadingList = true
        }
        Prefs.recommendedReadingListUpdateFrequency = frequency
        _uiState.value = _uiState.value.copy(updateFrequency = frequency)
    }

    fun toggleNotification(enabled: Boolean) {
        Prefs.isRecommendedReadingListNotificationEnabled = enabled
        _uiState.value = _uiState.value.copy(isRecommendedReadingListNotificationEnabled = enabled)
    }

    fun updateRecommendedReadingListSource(source: RecommendedReadingListSource) {
        Prefs.recommendedReadingListSource = source
        _uiState.value = _uiState.value.copy(recommendedReadingListSource = source)
    }

    fun generateRecommendedReadingList() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _resetUiState.value = Resource.Error(throwable)
        }) {
            _resetUiState.value = Resource.Loading()
            if (Prefs.resetRecommendedReadingList) {
                RecommendedReadingListHelper.generateRecommendedReadingList(true)
            }
            _resetUiState.value = Resource.Success(true)
        }
    }
}

data class RecommendedReadingListSettingsState(
    val isRecommendedReadingListEnabled: Boolean = Prefs.isRecommendedReadingListEnabled,
    val articlesNumber: Int = Prefs.recommendedReadingListArticlesNumber,
    val updateFrequency: RecommendedReadingListUpdateFrequency = Prefs.recommendedReadingListUpdateFrequency,
    val recommendedReadingListSource: RecommendedReadingListSource = Prefs.recommendedReadingListSource,
    val isRecommendedReadingListNotificationEnabled: Boolean = Prefs.isRecommendedReadingListNotificationEnabled
)
