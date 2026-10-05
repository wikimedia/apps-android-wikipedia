package org.wikipedia.search.semantic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.search.SearchResult
import org.wikipedia.util.UiState

class SemanticSearchResultsViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val semanticBatchSize = 8

    var searchQuery = savedStateHandle.get<String>(SemanticSearchResultsDialog.ARG_SEARCH_QUERY).orEmpty()
    var languageCode = savedStateHandle.get<String>(SemanticSearchResultsDialog.ARG_LANGUAGE_CODE).orEmpty().ifEmpty { WikipediaApp.instance.languageState.appLanguageCode }
    val invokeSource = savedStateHandle.get<Constants.InvokeSource>(Constants.INTENT_EXTRA_INVOKE_SOURCE) ?: Constants.InvokeSource.SEARCH
    private val showFeedback = savedStateHandle.get<Boolean>(SemanticSearchResultsDialog.ARG_SHOW_FEEDBACK) ?: false

    private var _semanticSearchResultsState = MutableStateFlow<UiState<List<SearchResult>>>(UiState.Loading)
    val semanticSearchResultsState = _semanticSearchResultsState.asStateFlow()

    private val _feedbackState = MutableStateFlow(FeedbackState())
    val feedbackState = _feedbackState.asStateFlow()

    init {
        loadSemanticSearchResults()
        if (showFeedback) {
            viewModelScope.launch {
                delay(FEEDBACK_DISPLAY_DELAY_MILLIS)
                semanticSearchResultsState.first { it is UiState.Success && it.data.isNotEmpty() }
                _feedbackState.update { it.copy(isVisible = true) }
            }
        }
    }

    fun selectFeedbackRating(isPositive: Boolean) {
        _feedbackState.update { it.copy(isPositive = isPositive) }
    }

    fun submitFeedback() {
        _feedbackState.update { it.copy(isVisible = false) }
    }

    fun deferFeedbackToArticle(): Boolean {
        if (!_feedbackState.value.isVisible) {
            return false
        }
        _feedbackState.update { it.copy(isVisible = false) }
        return true
    }

    @OptIn(FlowPreview::class)
    fun loadSemanticSearchResults() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _semanticSearchResultsState.value = UiState.Error(throwable)
        }) {
            _semanticSearchResultsState.value = UiState.Loading

            if (searchQuery.isEmpty() || languageCode.isEmpty()) {
                _semanticSearchResultsState.value = UiState.Success(emptyList())
                return@launch
            }

            val wikiSite = WikiSite.forLanguageCode(languageCode)

            val semanticResponse = ServiceFactory.get(wikiSite).fullTextSearchResponse(searchQuery, semanticBatchSize, 0, semanticSearchType = "hl")

            if (!semanticResponse.isSuccessful) {
                _semanticSearchResultsState.value = UiState.Success(emptyList())
                return@launch
            }

            val pages = semanticResponse.body()?.query?.pages?.sortedBy { it.index }.orEmpty()
            val semanticResults = pages.map { page ->
                SearchResult(
                    page = page,
                    wiki = wikiSite,
                    coordinates = page.coordinates,
                    type = SearchResult.SearchResultType.SEMANTIC,
                    indexInApiCall = page.index
                )
            }

            _semanticSearchResultsState.value = UiState.Success(semanticResults)

            semanticResults.forEach {
                launch {
                    loadAttribution(wikiSite, it)
                }
            }
        }
    }

    private suspend fun loadAttribution(wikiSite: WikiSite, searchResult: SearchResult) {
        val trustAndRelevance = runCatching {
            ServiceFactory.getCoreRest(wikiSite).getAttribution(searchResult.pageTitle.prefixedText)
        }.getOrNull()?.trustAndRelevance ?: return

        _semanticSearchResultsState.update { state ->
            if (state !is UiState.Success) {
                return@update state
            }
            UiState.Success(state.data.map {
                if (it.pageTitle == searchResult.pageTitle) {
                    it.copy(
                        editCounts = trustAndRelevance.contributorCounts,
                        referenceCounts = trustAndRelevance.referenceCount,
                        lastUpdated = trustAndRelevance.lastUpdated
                    )
                } else {
                    it
                }
            })
        }
    }

    data class FeedbackState(
        val isVisible: Boolean = false,
        val isPositive: Boolean? = null
    )

    companion object {
        private const val FEEDBACK_DISPLAY_DELAY_MILLIS = 3000L
    }
}
