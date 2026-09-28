package org.wikipedia.search

import androidx.core.net.toUri
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.page.PageTitle

class SearchResultsViewModel : ViewModel() {

    private val batchSize = 10
    private val delayMillis = 200L
    var countsPerLanguageCode = mutableListOf<Pair<String, Int>>()

    lateinit var invokeSource: Constants.InvokeSource

    private val _searchTerm = MutableStateFlow<String?>(null)
    var searchTerm = _searchTerm.asStateFlow()

    private val _searchTermForLogging = MutableSharedFlow<String?>()
    var searchTermForLogging = _searchTermForLogging.asSharedFlow()
    private val _lexicalResultsForLogging = MutableStateFlow<List<SearchResult>?>(null)
    var lexicalResultsForLogging = _lexicalResultsForLogging.asStateFlow()

    private var _languageCode = MutableStateFlow(WikipediaApp.instance.languageState.appLanguageCode)
    var languageCode = _languageCode.asStateFlow()

    private var _refreshSearchResults = MutableStateFlow(0)

    private var lastXSearchIdPrefix = ""
    private var lastXSearchIdFullText = ""

    @OptIn(
        FlowPreview::class,
        ExperimentalCoroutinesApi::class
    ) // TODO: revisit if the debounce method changed.
    val searchResultsFlow =
        combine(_searchTerm.debounce(delayMillis), _languageCode, _refreshSearchResults) { term, lang, _ ->
            Pair(term, lang)
        }.flatMapLatest { (term, lang) ->
            _searchTermForLogging.emit(term)
            val repository = StandardSearchRepository()
            Pager(PagingConfig(pageSize = batchSize)) {
                SearchResultsPagingSource(
                    searchTerm = term,
                    languageCode = lang,
                    countsPerLanguageCode = countsPerLanguageCode,
                    invokeSource = invokeSource,
                    repository = repository,
                    onFirstPageLoaded = { result ->
                        lastXSearchIdPrefix = result.xSearchIdPrefix.orEmpty()
                        lastXSearchIdFullText = result.xSearchIdFullText.orEmpty()
                        _lexicalResultsForLogging.value = result.results
                    }
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

    fun getStandardEventActionContext(result: SearchResult? = null): Map<String, Any> {
        return buildMap {
            put("search_id_pre", lastXSearchIdPrefix)
            put("search_id_ful", lastXSearchIdFullText)
            if (result != null) {
                put("position", result.indexInApiCall)
                put("type", result.type)
            }
        }
    }

    class SearchResultsPagingSource(
        private val searchTerm: String?,
        private val languageCode: String?,
        private var countsPerLanguageCode: MutableList<Pair<String, Int>>,
        private var invokeSource: Constants.InvokeSource,
        private val repository: SearchRepository<StandardSearchResults>,
        private val onFirstPageLoaded: (StandardSearchResults) -> Unit
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

                if (params.key == null) {
                    onFirstPageLoaded(result)
                }

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
            type: SearchResult.SearchResultType
        ): List<SearchResult> {
            return response?.query?.pages?.let { list ->
                (if (invokeSource == Constants.InvokeSource.PLACES)
                    list.filter { it.coordinates != null } else list).sortedBy { it.index }
                    .map { SearchResult(it, wikiSite, it.coordinates, type, indexInApiCall = it.index) }
            } ?: emptyList()
        }

        fun buildList(
            response: SemanticSearchResults,
            wikiSite: WikiSite,
            type: SearchResult.SearchResultType
        ): List<SearchResult> {
            return response.results.mapIndexed { index, result ->
                SearchResult(PageTitle.titleForUri(result.url.toUri(), wikiSite), searchResultType = type, snippet = postProcessSectionText(result.sectionText), indexInApiCall = index + 1)
            }
        }

        fun postProcessSectionText(text: String): String {
            // TODO: remove this when server-side parsing is done.
            val bold = Regex("'''(.*?)'''", RegexOption.DOT_MATCHES_ALL)
            val italic = Regex("''(.*?)''", RegexOption.DOT_MATCHES_ALL)
            val emptyParens = Regex("""\([\s,.;]*\)""")
            return text
                .replace(emptyParens, "")
                .replace(bold, "<b>\$1</b>")
                .replace(italic, "<i>\$1</i>")
        }
    }
}
