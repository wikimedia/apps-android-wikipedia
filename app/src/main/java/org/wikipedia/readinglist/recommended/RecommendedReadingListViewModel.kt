package org.wikipedia.readinglist.recommended

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.database.AppDatabase
import org.wikipedia.settings.Prefs
import org.wikipedia.util.Resource

class RecommendedReadingListViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    val fromSettings = savedStateHandle.get<Boolean>(RecommendedReadingListOnboardingActivity.EXTRA_FROM_SETTINGS) == true

    private val _uiSourceState = MutableStateFlow(Resource<SourceSelectionUiState>())
    val uiSourceState: StateFlow<Resource<SourceSelectionUiState>> = _uiSourceState.asStateFlow()

    init {
        setupSourceSelection()
    }

    fun setupSourceSelection() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _uiSourceState.value = Resource.Error(throwable)
        }) {
            _uiSourceState.value = Resource.Loading()
            val isSavedOptionEnabled = AppDatabase.instance.readingListPageDao().getPagesCount() > 0
            val isHistoryOptionEnabled = AppDatabase.instance.historyEntryDao().getHistoryCount() > 0
            val selectedSource = Prefs.recommendedReadingListSource
            _uiSourceState.value = Resource.Success(
                SourceSelectionUiState(
                    isSavedOptionEnabled = isSavedOptionEnabled,
                    isHistoryOptionEnabled = isHistoryOptionEnabled,
                    selectedSource = selectedSource
                )
            )
        }
    }

    fun updateSourceSelection(newSource: RecommendedReadingListSource) {
        val stateValue = _uiSourceState.value
        if (stateValue is Resource.Success) {
            _uiSourceState.value = Resource.Success(
                SourceSelectionUiState(
                    isSavedOptionEnabled = stateValue.data.isSavedOptionEnabled,
                    isHistoryOptionEnabled = stateValue.data.isHistoryOptionEnabled,
                    selectedSource = newSource
                )
            )
        }
    }

    fun saveSourceSelection(): Boolean {
        val stateValue = _uiSourceState.value
        if (stateValue is Resource.Success) {
            val selectedSource = stateValue.data.selectedSource
            Prefs.recommendedReadingListSource = selectedSource
            if (selectedSource == RecommendedReadingListSource.INTERESTS) {
                return true
            }
        }
        return false
    }

    data class SourceSelectionUiState(
        val isSavedOptionEnabled: Boolean,
        val isHistoryOptionEnabled: Boolean,
        val selectedSource: RecommendedReadingListSource
    )
}
