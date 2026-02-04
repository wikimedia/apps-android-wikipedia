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

class SearchResultsViewModel : ViewModel() {

    private val batchSize = 10
    private val delayMillis = 200L
    var countsPerLanguageCode = mutableListOf<Pair<String, Int>>()

    lateinit var invokeSource: Constants.InvokeSource

    private val _searchTerm = MutableStateFlow<String?>(null)
    var searchTerm = _searchTerm.asStateFlow()

    private var _languageCode = MutableStateFlow<String?>(null)
    var languageCode = _languageCode.asStateFlow()

    private var _refreshSearchResults = MutableStateFlow(0)

    @OptIn(
        FlowPreview::class,
        ExperimentalCoroutinesApi::class
    ) // TODO: revisit if the debounce method changed.
    val searchResultsFlow =
        combine(_searchTerm, _languageCode, _refreshSearchResults) { term, lang, _ ->
            Pair(term, lang)
        }.debounce(delayMillis).flatMapLatest { (term, lang) ->
            val repository = StandardSearchRepository()
            Pager(PagingConfig(pageSize = batchSize)) {
                SearchResultsPagingSource(
                    term,
                    lang,
                    countsPerLanguageCode,
                    invokeSource,
                    repository
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
}
