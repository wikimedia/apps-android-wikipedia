package org.wikipedia.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.util.UiState

class SearchResultsViewModel : ViewModel() {

    private val batchSize = 10
    private val delayMillis = 200L
    var countsPerLanguageCode = mutableListOf<Pair<String, Int>>()

    lateinit var invokeSource: Constants.InvokeSource

    private val _searchTerm = MutableStateFlow<String?>(null)
    var searchTerm = _searchTerm.asStateFlow()

    private var _languageCode = MutableStateFlow<String?>(null)
    var languageCode = _languageCode.asStateFlow()

    private var _hybridSearchResultState = MutableStateFlow<UiState<HybridUiState>>(UiState.Loading)
    val hybridSearchResultState = _hybridSearchResultState.asStateFlow()

    private var _refreshSearchResults = MutableStateFlow(0)

    val getTestGroup get() = HybridSearchAbCTest().getGroupName()

    val isHybridSearchExperimentOn get() = HybridSearchAbCTest().isHybridSearchEnabled(languageCode.value)

    @OptIn(
        FlowPreview::class,
        ExperimentalCoroutinesApi::class
    ) // TODO: revisit if the debounce method changed.
    val searchResultsFlow =
        combine(_searchTerm.debounce(delayMillis), _languageCode, _refreshSearchResults) { term, lang, _ ->
            Pair(term, lang)
        }.flatMapLatest { (term, lang) ->
            val repository = StandardSearchRepository()
            Pager(PagingConfig(pageSize = batchSize)) {
                SearchResultsPagingSource(
                    searchTerm = term,
                    languageCode = lang,
                    countsPerLanguageCode = countsPerLanguageCode,
                    invokeSource = invokeSource,
                    repository = repository
                )
            }.flow
        }.cachedIn(viewModelScope)

    @OptIn(FlowPreview::class)
    fun loadHybridSearchResults() {
       viewModelScope.launch {
            _hybridSearchResultState.value = UiState.Loading

            val lexicalBatchSize = 3
            val semanticBatchSize = 3

            val term = _searchTerm.debounce(delayMillis).first()
            val lang = _languageCode.value

            if (term.isNullOrEmpty() || lang.isNullOrEmpty()) {
                _hybridSearchResultState.value = UiState.Success(HybridUiState())
                return@launch
            }

            val wikiSite = WikiSite.forLanguageCode(lang)

            val lexicalDeferred = async {
                runCatching {
                    val lexicalSearchResults = mutableListOf<SearchResult>()
                    // prefix + fulltext search results for at most 3 results.
                    var response = ServiceFactory.get(wikiSite).prefixSearch(term, lexicalBatchSize, 0)
                    lexicalSearchResults.addAll(buildList(response, invokeSource, wikiSite))
                    if (lexicalSearchResults.size < lexicalBatchSize) {
                        response = ServiceFactory.get(wikiSite).fullTextSearch(term, lexicalBatchSize, 0)
                        lexicalSearchResults.addAll(buildList(response, invokeSource, wikiSite))
                    }

                    lexicalSearchResults
                }
            }

            val semanticDeferred = async {
                runCatching {
                    val response = ServiceFactory.get(wikiSite)
                        .semanticSearch(term, semanticBatchSize)
                    buildList(response, invokeSource, wikiSite, SearchResult.SearchResultType.SEMANTIC)
                }
            }

            val lexicalResult = lexicalDeferred.await()
            val semanticResult = semanticDeferred.await()

            if (lexicalResult.isFailure && semanticResult.isFailure) {
                _hybridSearchResultState.value = UiState.Error(Throwable())
                return@launch
            }

            val lexicalList = lexicalResult.getOrElse { emptyList() }
                .distinctBy { it.pageTitle.prefixedText }
            val semanticList = semanticResult.getOrElse { emptyList() }

            _hybridSearchResultState.value = UiState.Success(HybridUiState(
                lexicalList = lexicalList,
                semanticList = semanticList,
            ))
        }
    }

    fun updateSearchTerm(term: String?) {
        _searchTerm.value = term
    }

    fun updateLanguageCode(code: String) {
        _languageCode.value = code
    }

    fun refreshSearchResults() {
        _refreshSearchResults.value += 1
    }

    class SearchResultsPagingSource(
        private val searchTerm: String?,
        private val languageCode: String?,
        private var countsPerLanguageCode: MutableList<Pair<String, Int>>,
        private var invokeSource: Constants.InvokeSource,
        private val repository: SearchRepository<StandardSearchResults>,
    ) : PagingSource<Int, SearchResult>() {

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchResult> {
            return try {
                if (searchTerm.isNullOrEmpty() || languageCode.isNullOrEmpty()) {
                    return LoadResult.Page(emptyList(), null, null)
                }

                val result = repository.search(
                    searchTerm = searchTerm,
                    languageCode = languageCode,
                    invokeSource = invokeSource,
                    continuation = params.key,
                    batchSize = params.loadSize,
                    isPrefixSearch = params.key == null,
                    countsPerLanguageCode = countsPerLanguageCode
                )

                return LoadResult.Page(
                    result.results,
                    null,
                    result.continuation
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }

        override fun getRefreshKey(state: PagingState<Int, SearchResult>): Int? {
            return null
        }
    }

    companion object {
        fun buildList(
            response: MwQueryResponse?,
            invokeSource: Constants.InvokeSource,
            wikiSite: WikiSite,
            type: SearchResult.SearchResultType = SearchResult.SearchResultType.SEARCH
        ): List<SearchResult> {
            return response?.query?.pages?.let { list ->
                (if (invokeSource == Constants.InvokeSource.PLACES)
                    list.filter { it.coordinates != null } else list).sortedBy { it.index }
                    .map { SearchResult(it, wikiSite, it.coordinates, type) }
            } ?: emptyList()
        }
    }
}

data class HybridSearchConfig(
    val isHybridSearchExperimentOn: Boolean = false,
    val onTitleClick: (SearchResult) -> Unit,
    val onSuggestionTitleClick: (String?) -> Unit
)

data class HybridUiState(
    val lexicalList: List<SearchResult> = emptyList(),
    val semanticList: List<SearchResult> = emptyList()
)
