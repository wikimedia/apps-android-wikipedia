package org.wikipedia.search.semantic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wikimedia.testkitchen.instrument.Funnel
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter

class SemanticSearchFeedbackViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    val languageCode = savedStateHandle.get<String>(SemanticSearchFeedbackDialog.ARG_LANGUAGE_CODE)
        .orEmpty().ifEmpty { WikipediaApp.instance.languageState.appLanguageCode }

    val searchIdSemantic = savedStateHandle.get<String>(SemanticSearchFeedbackDialog.ARG_SEARCH_ID_SEMANTIC).orEmpty()

    val instrument = TestKitchenAdapter.client.getInstrument("apps-search")
        .setDefaultActionSource("search")
        .continueFunnel(savedStateHandle.get<Funnel>(SemanticSearchFeedbackDialog.ARG_FUNNEL))

    private val _selectedRating = MutableStateFlow<Boolean?>(null)
    val selectedRating = _selectedRating.asStateFlow()

    private val _feedbackSubmitted = MutableStateFlow<Boolean?>(null)
    val feedbackSubmitted = _feedbackSubmitted.asStateFlow()

    fun selectRating(isPositive: Boolean) {
        _selectedRating.value = isPositive
    }

    fun submitFeedback() {
        _feedbackSubmitted.value = true
    }
}
