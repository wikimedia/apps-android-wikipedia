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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import org.wikipedia.Constants

class HybridSearchResultsViewModel : ViewModel() {

    private val batchSize = 3
    private val delayMillis = 200L

    lateinit var invokeSource: Constants.InvokeSource

    private val _searchTerm = MutableStateFlow<String?>(null)
    var searchTerm = _searchTerm.asStateFlow()

    private var _languageCode = MutableStateFlow<String?>(null)
    var languageCode = _languageCode.asStateFlow()

    private var _refreshSearchResults = MutableStateFlow(0)

    val getTestGroup get() = HybridSearchAbTest().getGroupName()
    val isHybridSearchExperimentOn get() = HybridSearchAbTest().isHybridSearchEnabled(languageCode.value)
    @OptIn(
        FlowPreview::class,
        ExperimentalCoroutinesApi::class
    ) // TODO: revisit if the debounce method changed.
    val standardSearchResultsFlow =
        combine(_searchTerm.debounce(delayMillis), _languageCode, _refreshSearchResults) { term, lang, _ ->
            Pair(term, lang)
        }.flatMapLatest { (term, lang) ->
            val repository = StandardSearchRepository()
            Pager(PagingConfig(pageSize = batchSize, initialLoadSize = batchSize)) {
                SearchResultsViewModel.SearchResultsPagingSource(
                    searchTerm = term,
                    languageCode = lang,
                    countsPerLanguageCode = mutableListOf(),
                    searchInLanguages = false,
                    invokeSource = invokeSource,
                    repository = repository,
                    isHybridSearch = true
                )
            }.flow
        }.cachedIn(viewModelScope)

    // TODO: depends on how the API is designed, may need a separate repository for hybrid search
    @OptIn(
        FlowPreview::class,
        ExperimentalCoroutinesApi::class
    ) // TODO: revisit if the debounce method changed.
    val semanticSearchResultsFlow =
        combine(_searchTerm.debounce(delayMillis), _languageCode, _refreshSearchResults) { term, lang, _ ->
            Pair(term, lang)
        }.flatMapLatest { (term, lang) ->
            val repository = HybridSearchRepository()
            Pager(PagingConfig(pageSize = batchSize, initialLoadSize = batchSize)) {
                SemanticSearchResultsPagingSource(
                    searchTerm = term,
                    languageCode = lang,
                    invokeSource = invokeSource,
                    repository = repository
                )
            }.flow
        }.cachedIn(viewModelScope)

    fun updateSearchTerm(term: String?) {
        _searchTerm.value = term
    }

    fun updateLanguageCode(code: String) {
        _languageCode.value = code
    }

    fun refreshSearchResults() {
        _refreshSearchResults.value += 1
    }

    class SemanticSearchResultsPagingSource(
        private val searchTerm: String?,
        private val languageCode: String?,
        private var invokeSource: Constants.InvokeSource,
        private val repository: SearchRepository<HybridSearchResults>,
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
                    continuation = null,
                    batchSize = params.loadSize,
                    isPrefixSearch = false
                )

                return LoadResult.Page(
                    result.results,
                    null,
                    null
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
}
