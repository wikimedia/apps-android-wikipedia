package org.wikipedia.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.withContext
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.SearchFunnel
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import java.util.*
import java.util.concurrent.TimeUnit

class SearchResultsViewModel(searchFunnel: SearchFunnel?) : ViewModel() {

    private val batchSize = 20
    private val delayMillis = 200L
    var resultsCount = mutableListOf<Int>()
    var searchTerm: String? = null
    var languageCode: String? = null

    @OptIn(FlowPreview::class) // TODO: revisit if the debounce method changed.
    val searchResultsFlow = Pager(PagingConfig(pageSize = batchSize, initialLoadSize = batchSize)) {
        SearchResultsPagingSource(searchTerm, languageCode, resultsCount, searchFunnel)
    }.flow.debounce(delayMillis).cachedIn(viewModelScope)

    class SearchResultsPagingSource(
            val searchTerm: String?,
            val languageCode: String?,
            var resultsCount: MutableList<Int>?,
            private val searchFunnel: SearchFunnel?
    ) : PagingSource<MwQueryResponse.Continuation, SearchResult>() {

        private val batchSize = 20
        private var prefixSearch = true
        private var startTime: Long = 0

        override suspend fun load(params: LoadParams<MwQueryResponse.Continuation>): LoadResult<MwQueryResponse.Continuation, SearchResult> {
            return try {
                // The default offset is 0 but we send the initial offset from 1 to prevent showing the same talk page from the results.
                if (searchTerm.isNullOrEmpty() || languageCode.isNullOrEmpty()) {
                    return LoadResult.Page(emptyList(), null, null)
                }

                val wikiSite = WikiSite.forLanguageCode(languageCode)
                var response: MwQueryResponse? = null
                var readingListSearch = SearchResults()
                var historySearch = SearchResults()
                if (prefixSearch) {
                    if (searchTerm.length > 2) {
                        readingListSearch = withContext(Dispatchers.IO) {
                            async {
                                AppDatabase.instance.readingListPageDao().findPageForSearchQueryInAnyList(searchTerm)
                            }
                        }.await()

                        historySearch = withContext(Dispatchers.IO) {
                            async {
                                AppDatabase.instance.historyEntryWithImageDao().findHistoryItem(searchTerm)
                            }
                        }.await()
                    }
                    response = ServiceFactory.get(wikiSite).prefixSearch(searchTerm, params.loadSize, params.key?.gpsoffset)
                    prefixSearch = false
                }

                if (response?.query?.pages == null) {
                    startTime = System.nanoTime()
                    // Prevent using continuation string from prefix search
                    val continuation = if (params.key?.continuation?.contains("description") == true) null else params.key?.continuation
                    response = ServiceFactory.get(wikiSite)
                        .fullTextSearch(searchTerm, params.key?.gsroffset?.toString(), params.loadSize, continuation)
                } else {
                    // Log prefix search
                    searchFunnel?.searchResults(true, response.query?.pages?.size ?: 0, displayTime(startTime), languageCode)
                }

                val resultList = mutableListOf<SearchResult>()
                addSearchResultsFromTabs(searchTerm, resultList)

                resultList.addAll(readingListSearch.results.filterNot { res ->
                    resultList.map { it.pageTitle.prefixedText }
                        .contains(res.pageTitle.prefixedText)
                }.take(1))

                resultList.addAll(historySearch.results.filterNot { res ->
                    resultList.map { it.pageTitle.prefixedText }
                        .contains(res.pageTitle.prefixedText)
                }.take(1))

                val searchResults = response.query?.pages?.let { list ->
                    searchFunnel?.searchResults(true, response.query?.pages?.size ?: 0, displayTime(startTime), languageCode)
                    list.sortedBy { it.index }.map {
                        SearchResult(it, wikiSite)
                    }
                } ?: emptyList()

                if (searchResults.isEmpty() && response.continuation == null) {
                    resultsCount?.clear()
                    L.d("Start checking result count...")
                    WikipediaApp.instance.languageState.appLanguageCodes.forEach { langCode ->
                        if (langCode == languageCode) {
                            resultsCount?.add(0)
                        } else {
                            val prefixSearchResponse = withContext(Dispatchers.IO) {
                                ServiceFactory.get(WikiSite.forLanguageCode(langCode))
                                    .prefixSearch(searchTerm, batchSize, 0)
                            }
                            prefixSearchResponse.query?.pages?.let {
                                resultsCount?.add(it.size)
                            } ?: run {
                                val fullTextSearchResponse = withContext(Dispatchers.IO) {
                                    ServiceFactory.get(WikiSite.forLanguageCode(langCode))
                                        .fullTextSearch(searchTerm, batchSize.toString(), batchSize, null)
                                }
                                resultsCount?.add(fullTextSearchResponse.query?.pages?.size ?: 0)
                            }
                        }
                    }
                    // make a singleton list if all results are empty.
                    if (resultsCount?.sum() == 0) {
                        resultsCount = mutableListOf(0)
                    }
                }

                resultList.addAll(searchResults)

                return LoadResult.Page(resultList, null, response.continuation)
            } catch (e: Exception) {
                searchFunnel?.searchError(!prefixSearch, displayTime(startTime), languageCode)
                LoadResult.Error(e)
            }
        }

        override fun getRefreshKey(state: PagingState<MwQueryResponse.Continuation, SearchResult>): MwQueryResponse.Continuation? {
            return null
        }

        private fun addSearchResultsFromTabs(searchTerm: String, resultList: MutableList<SearchResult>) {
            if (searchTerm.length < 2) {
                return
            }
            WikipediaApp.instance.tabList.forEach { tab ->
                tab.backStackPositionTitle?.let {
                    if (StringUtil.fromHtml(it.displayText).toString().lowercase(Locale.getDefault()).contains(searchTerm.lowercase(
                            Locale.getDefault()))) {
                        resultList.add(SearchResult(it, SearchResult.SearchResultType.TAB_LIST))
                        return
                    }
                }
            }
        }

        private fun displayTime(startTime: Long): Int {
            return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime).toInt()
        }
    }

    class Factory(private val searchFunnel: SearchFunnel?) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchResultsViewModel(searchFunnel) as T
        }
    }
}
