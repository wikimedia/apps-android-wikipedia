package org.wikipedia.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
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
    private val totalResults = mutableListOf<SearchResult>()
    var resultsCount = mutableListOf<Int>()
    var searchTerm: String? = null
    var languageCode: String? = null
    lateinit var invokeSource: Constants.InvokeSource

    @OptIn(FlowPreview::class) // TODO: revisit if the debounce method changed.
    val searchResultsFlow = Pager(PagingConfig(pageSize = batchSize, initialLoadSize = batchSize)) {
        SearchResultsPagingSource(searchTerm, languageCode, resultsCount, totalResults, invokeSource)
    }.flow.debounce(delayMillis).map { pagingData ->
        pagingData.filter { searchResult ->
            totalResults.find { it.pageTitle.prefixedText == searchResult.pageTitle.prefixedText } == null
        }.map {
            totalResults.add(it)
            it
        }
    }.cachedIn(viewModelScope)

    fun clearResults() {
        resultsCount.clear()
        totalResults.clear()
    }

    class SearchResultsPagingSource(
        private val searchTerm: String?,
        private val languageCode: String?,
        private var resultsCount: MutableList<Int>?,
        private var totalResults: MutableList<SearchResult>?,
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
                                getSearchResultsFromTabs(searchTerm)
                            }, async {
                                AppDatabase.instance.historyEntryWithImageDao().findHistoryItem(searchTerm)
                            }, async {
                                AppDatabase.instance.readingListPageDao().findPageForSearchQueryInAnyList(searchTerm)
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
                    resultsCount?.clear()
                    WikipediaApp.instance.languageState.appLanguageCodes.forEach { langCode ->
                        if (langCode == languageCode) {
                            resultsCount?.add(0)
                        } else {
                            val prefixSearchResponse = ServiceFactory.get(WikiSite.forLanguageCode(langCode))
                                    .prefixSearch(searchTerm, params.loadSize, 0)
                            var countResultSize = 0
                            prefixSearchResponse.query?.pages?.let {
                                countResultSize = it.size
                            }
                            if (countResultSize == 0) {
                                val fullTextSearchResponse = ServiceFactory.get(WikiSite.forLanguageCode(langCode))
                                        .fullTextSearch(searchTerm, params.loadSize, null)
                                countResultSize = fullTextSearchResponse.query?.pages?.size ?: 0
                            }
                            resultsCount?.add(countResultSize)
                        }
                    }
                    // make a singleton list if all results are empty.
                    if (resultsCount?.sum() == 0) {
                        resultsCount = mutableListOf(0)
                    }
                }

                return LoadResult.Page(resultList.distinctBy { it.pageTitle.prefixedText }, null, continuation)
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }

        override fun getRefreshKey(state: PagingState<Int, SearchResult>): Int? {
            prefixSearch = true
            totalResults?.clear()
            return null
        }

        private fun getSearchResultsFromTabs(searchTerm: String): SearchResults {
            WikipediaApp.instance.tabList.forEach { tab ->
                tab.backStackPositionTitle?.let {
                    if (StringUtil.fromHtml(it.displayText).contains(searchTerm, true)) {
                        return SearchResults(mutableListOf(SearchResult(it, SearchResult.SearchResultType.TAB_LIST)))
                    }
                }
            }
            return SearchResults()
        }
    }
}
