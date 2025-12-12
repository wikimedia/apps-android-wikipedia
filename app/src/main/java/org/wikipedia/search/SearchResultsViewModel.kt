package org.wikipedia.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.util.StringUtil

class SearchResultsViewModel : ViewModel() {

    private val batchSize = 20
    private val delayMillis = 200L
    var countsPerLanguageCode = mutableListOf<Pair<String, Int>>()

    lateinit var invokeSource: Constants.InvokeSource

    private val _searchTerm = MutableStateFlow<String?>(null)
    var searchTerm = _searchTerm.asStateFlow()

    private var _languageCode = MutableStateFlow<String?>(null)
    var languageCode = _languageCode.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class) // TODO: revisit if the debounce method changed.
    val searchResultsFlow = combine(_searchTerm, _languageCode) { term, lang ->
        Pair(term, lang)
    }.debounce(delayMillis).flatMapLatest { (term, lang) ->
        Pager(PagingConfig(pageSize = batchSize, initialLoadSize = batchSize)) {
            SearchResultsPagingSource(term, lang, countsPerLanguageCode, invokeSource)
        }.flow
    }.cachedIn(viewModelScope)

    fun updateSearchTerm(term: String?) {
        _searchTerm.value = term
    }

    fun updateLanguageCode(code: String) {
        _languageCode.value = code
    }

    class SearchResultsPagingSource(
        private val searchTerm: String?,
        private val languageCode: String?,
        private var countsPerLanguageCode: MutableList<Pair<String, Int>>,
        private var invokeSource: Constants.InvokeSource
    ) : PagingSource<Int, SearchResult>() {

        private var prefixSearch = true

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchResult> {
            return try {
                if (searchTerm.isNullOrEmpty() || languageCode.isNullOrEmpty()) {
                    return LoadResult.Page(emptyList(), null, null)
                }

                var continuation: Int? = null
                val wikiSite = WikiSite.forLanguageCode(languageCode)
                var response: MwQueryResponse? = null
                val resultList = mutableListOf<SearchResult>()
                if (prefixSearch) {
                    if (searchTerm.length >= 2 && invokeSource != Constants.InvokeSource.PLACES) {
                        withContext(Dispatchers.IO) {
                            listOf(async {
                                getSearchResultsFromTabs(wikiSite, searchTerm)
                            }, async {
                                AppDatabase.instance.historyEntryWithImageDao().findHistoryItem(wikiSite, searchTerm)
                            }, async {
                                AppDatabase.instance.readingListPageDao().findPageForSearchQueryInAnyList(wikiSite, searchTerm)
                            }).awaitAll().forEach {
                                resultList.addAll(it.results.take(1))
                            }
                        }
                    }
                    response = ServiceFactory.get(wikiSite).prefixSearch(searchTerm, params.loadSize, 0)
                    continuation = 0
                    prefixSearch = false
                }

                resultList.addAll(response?.query?.pages?.let { list ->
                    (if (invokeSource == Constants.InvokeSource.PLACES)
                        list.filter { it.coordinates != null } else list).sortedBy { it.index }
                        .map { SearchResult(it, wikiSite, it.coordinates) }
                } ?: emptyList())

                if (resultList.size < params.loadSize) {
                    response = ServiceFactory.get(wikiSite)
                        .fullTextSearch(searchTerm, params.loadSize, params.key)
                    continuation = response.continuation?.gsroffset

                    resultList.addAll(response.query?.pages?.let { list ->
                        (if (invokeSource == Constants.InvokeSource.PLACES)
                            list.filter { it.coordinates != null } else list).sortedBy { it.index }
                            .map { SearchResult(it, wikiSite, it.coordinates) }
                    } ?: emptyList())
                }

                if (resultList.isEmpty() && response?.continuation == null) {
                    countsPerLanguageCode.clear()
                    WikipediaApp.instance.languageState.appLanguageCodes.forEach { langCode ->
                        var countResultSize = 0
                        if (langCode != languageCode) {
                            val prefixSearchResponse = ServiceFactory.get(WikiSite.forLanguageCode(langCode))
                                    .prefixSearch(searchTerm, params.loadSize, 0)
                            prefixSearchResponse.query?.pages?.let {
                                countResultSize = it.size
                            }
                            if (countResultSize == 0) {
                                val fullTextSearchResponse = ServiceFactory.get(WikiSite.forLanguageCode(langCode))
                                        .fullTextSearch(searchTerm, params.loadSize, null)
                                countResultSize = fullTextSearchResponse.query?.pages?.size ?: 0
                            }
                        }
                        countsPerLanguageCode.add(langCode to countResultSize)
                    }
                }

                return LoadResult.Page(resultList.distinctBy { it.pageTitle.prefixedText }, null, continuation)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }

        override fun getRefreshKey(state: PagingState<Int, SearchResult>): Int? {
            prefixSearch = true
            return null
        }

        private fun getSearchResultsFromTabs(wikiSite: WikiSite, searchTerm: String): SearchResults {
            WikipediaApp.instance.tabList.forEach { tab ->
                tab.backStackPositionTitle?.let {
                    if (wikiSite == it.wikiSite && StringUtil.fromHtml(it.displayText).contains(searchTerm, true)) {
                        return SearchResults(mutableListOf(SearchResult(it, SearchResult.SearchResultType.TAB_LIST)))
                    }
                }
            }
            return SearchResults()
        }
    }
}
