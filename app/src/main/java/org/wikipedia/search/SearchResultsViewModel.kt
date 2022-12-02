package org.wikipedia.search

import androidx.collection.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.withContext
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.util.StringUtil
import java.util.*

class SearchResultsViewModel : ViewModel() {

    private val batchSize = 20
    private val maxCacheSize = 4
    // TODO: add cache logic
    private val searchResultsCache = LruCache<String, MutableList<SearchResult>>(maxCacheSize)
    private val searchResultsCountCache = LruCache<String, List<Int>>(maxCacheSize)
    var resultsCount = mutableListOf<Int>()
    var searchTerm: String? = null
    var languageCode: String? = null
    val searchResultsFlow = Pager(PagingConfig(pageSize = batchSize)) {
        SearchResultsPagingSource(searchTerm, languageCode)
    }.flow.onEmpty {
        WikipediaApp.instance.languageState.appLanguageCodes.forEach { langCode ->
            if (langCode == languageCode) {
                resultsCount.add(0)
            } else {
                val prefixSearchResponse = withContext(Dispatchers.IO) {
                    ServiceFactory.get(WikiSite.forLanguageCode(langCode))
                        .prefixSearch(searchTerm, batchSize, 0)
                }
                prefixSearchResponse.query?.pages?.let {
                    resultsCount.add(it.size)
                } ?: run {
                    val fullTextSearchResponse = withContext(Dispatchers.IO) {
                        ServiceFactory.get(WikiSite.forLanguageCode(langCode))
                            .fullTextSearchMedia(searchTerm, batchSize.toString(), batchSize, null)
                    }
                    resultsCount.add(fullTextSearchResponse.query?.pages?.size ?: 0)
                }
            }
        }
        // make a singleton list if all results are empty.
        if (resultsCount.sum() == 0) {
            resultsCount = mutableListOf(0)
        }
    }.cachedIn(viewModelScope)

    class SearchResultsPagingSource(
            val searchTerm: String?,
            val languageCode: String?
    ) : PagingSource<MwQueryResponse.Continuation, SearchResult>() {

        private var prefixSearch = true

        override suspend fun load(params: LoadParams<MwQueryResponse.Continuation>): LoadResult<MwQueryResponse.Continuation, SearchResult> {
            return try {
                // TODO: add delay logic
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
                }

                if (response?.query?.pages == null) {
                    response = ServiceFactory.get(wikiSite)
                        .fullTextSearchMedia(searchTerm, params.key?.gsroffset?.toString(), params.loadSize, params.key?.continuation)

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
                    list.sortedBy { it.index }.map {
                        SearchResult(it, wikiSite)
                    }
                } ?: emptyList()

                resultList.addAll(searchResults)

                return LoadResult.Page(resultList, null, response.continuation)

            } catch (e: Exception) {
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
    }
}
