package org.wikipedia.search.semantic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.search.SearchResult
import org.wikipedia.search.SearchResultsViewModel
import org.wikipedia.util.UiState

class SemanticSearchResultsViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val semanticBatchSize = 3

    var searchQuery = savedStateHandle.get<String>(SemanticSearchResultsDialog.ARG_SEARCH_QUERY).orEmpty()
    var languageCode = savedStateHandle.get<String>(SemanticSearchResultsDialog.ARG_LANGUAGE_CODE).orEmpty().ifEmpty { WikipediaApp.instance.languageState.appLanguageCode }
    val invokeSource = savedStateHandle.get<Constants.InvokeSource>(Constants.INTENT_EXTRA_INVOKE_SOURCE) ?: Constants.InvokeSource.SEARCH

    // TODO: we'll probably need a separate data class for edit and reference counts.
    private var _semanticSearchResultState = MutableStateFlow<UiState<List<SearchResult>>>(UiState.Loading)
    val semanticSearchResultState = _semanticSearchResultState.asStateFlow()

    init {
        loadSemanticSearchResults()
    }

    @OptIn(FlowPreview::class)
    fun loadSemanticSearchResults() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _semanticSearchResultState.value = UiState.Error(throwable)
        }) {
            _semanticSearchResultState.value = UiState.Loading

            if (searchQuery.isEmpty() || languageCode.isEmpty()) {
                _semanticSearchResultState.value = UiState.Success(emptyList())
                return@launch
            }

            val wikiSite = WikiSite.forLanguageCode(languageCode)

            // TODO: adding additional API requests for edit counts and reference counts
            val semanticDeferred = async {
                runCatching {
                    val response = ServiceFactory.get(wikiSite).fullTextSearchResponse(searchQuery, semanticBatchSize, 0, isSemantic = true)
                    SearchResultsViewModel.buildList(response.body(), invokeSource, wikiSite, type = SearchResult.SearchResultType.SEMANTIC)
                }
            }

            val semanticResult = semanticDeferred.await()

            if (semanticResult.isFailure) {
                _semanticSearchResultState.value = UiState.Error(Throwable())
                return@launch
            }

            _semanticSearchResultState.value = UiState.Success(semanticResult.getOrElse { emptyList() })
        }
    }
}
