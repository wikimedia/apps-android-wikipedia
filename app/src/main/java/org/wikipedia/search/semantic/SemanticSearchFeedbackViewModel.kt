package org.wikipedia.search.semantic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wikipedia.WikipediaApp

class SemanticSearchFeedbackViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    val languageCode = savedStateHandle.get<String>(SemanticSearchFeedbackDialog.ARG_LANGUAGE_CODE)
        .orEmpty().ifEmpty { WikipediaApp.instance.languageState.appLanguageCode }

    private val _selectedRating = MutableStateFlow<Boolean?>(null)
    val selectedRating = _selectedRating.asStateFlow()

    fun selectRating(isPositive: Boolean) {
        _selectedRating.value = isPositive
    }
}
