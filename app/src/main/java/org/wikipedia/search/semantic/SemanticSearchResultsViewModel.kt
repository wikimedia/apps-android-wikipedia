package org.wikipedia.search.semantic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.search.SearchResult
import org.wikipedia.util.UiState

class SemanticSearchResultsViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val semanticBatchSize = 3

    var searchQuery = savedStateHandle.get<String>(SemanticSearchResultsDialog.ARG_SEARCH_QUERY).orEmpty()
    var languageCode = savedStateHandle.get<String>(SemanticSearchResultsDialog.ARG_LANGUAGE_CODE).orEmpty().ifEmpty { WikipediaApp.instance.languageState.appLanguageCode }
    val invokeSource = savedStateHandle.get<Constants.InvokeSource>(Constants.INTENT_EXTRA_INVOKE_SOURCE) ?: Constants.InvokeSource.SEARCH

    // TODO: we'll probably need a separate data class for edit and reference counts.
    private var _semanticSearchResultState = MutableStateFlow<UiState<List<SearchResult>>>(UiState.Loading)
    val semanticSearchResultState = _semanticSearchResultState.asStateFlow()

    val quotationMarkMap = mapOf(
        "ja" to "『",
        "ar" to "«",
        "fr" to "❞"
    )

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

            val semanticResponse = ServiceFactory.get(wikiSite).fullTextSearchResponse(searchQuery, semanticBatchSize, 0, semanticSearchType = "hl")

            val semanticResult = semanticResponse.body()?.query?.pages?.sortedBy { it.index }
                ?.map { page ->
                    async {
                        val pageAttributionResponse = ServiceFactory.getCoreRest(wikiSite).getAttribution(page.title)
                        SearchResult(
                            page = page,
                            wiki = wikiSite,
                            coordinates = page.coordinates,
                            type = SearchResult.SearchResultType.SEMANTIC,
                            indexInApiCall = page.index,
                            editCounts = pageAttributionResponse.trustAndRelevance?.contributorCounts,
                            referenceCounts = pageAttributionResponse.trustAndRelevance?.referenceCount
                        )
                    }
                }?.awaitAll() ?: emptyList()

            _semanticSearchResultState.value = UiState.Success(semanticResult)
        }
    }
}
