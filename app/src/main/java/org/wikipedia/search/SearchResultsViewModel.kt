package org.wikipedia.search

import android.os.Bundle
import androidx.collection.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.util.StringUtil
import java.util.*

class SearchResultsViewModel(bundle: Bundle) : ViewModel() {

    private val batchSize = 20
    private val maxCacheSize = 4
    private val searchResultsCache = LruCache<String, MutableList<SearchResult>>(maxCacheSize)
    private val searchResultsCountCache = LruCache<String, List<Int>>(maxCacheSize)
    var searchTerm: String? = null
    var languageCode: String? = null
    val searchResultsFlow = Pager(PagingConfig(pageSize = batchSize)) {
        SearchResultsPagingSource(searchTerm, languageCode)
    }.flow.map { pagingData ->
        if (searchTerm.isNullOrEmpty() || languageCode.isNullOrEmpty()) {
            pagingData
        } else {
            val searchQuery = searchTerm!!

            var readingListSearch = SearchResults()
            var historySearch = SearchResults()
            if (searchQuery.length > 2) {

                readingListSearch = withContext(Dispatchers.IO) {
                    async {
                        AppDatabase.instance.readingListPageDao().findPageForSearchQueryInAnyList(searchQuery)
                    }
                }.await()

                historySearch = withContext(Dispatchers.IO) {
                    async {
                        AppDatabase.instance.historyEntryWithImageDao().findHistoryItem(searchQuery)
                    }
                }.await()
            }

            val resultList = mutableListOf<SearchResult>()
            addSearchResultsFromTabs(searchQuery, resultList)

            resultList.addAll(readingListSearch.results.filterNot { res ->
                resultList.map { it.pageTitle.prefixedText }
                    .contains(res.pageTitle.prefixedText)
            }.take(1))

            resultList.addAll(historySearch.results.filterNot { res ->
                resultList.map { it.pageTitle.prefixedText }
                    .contains(res.pageTitle.prefixedText)
            }.take(1))

            // TODO: verify this
            pagingData.insertHeaderItem(item = SearchResults(resultList))
        }
    }.cachedIn(viewModelScope)

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

    class SearchResultsPagingSource(
            val searchTerm: String?,
            val languageCode: String?
    ) : PagingSource<Int, SearchResults>() {

        private var prefixSearch = true

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchResults> {
            return try {
                // TODO: add delay logic
                // The default offset is 0 but we send the initial offset from 1 to prevent showing the same talk page from the results.
                if (searchTerm.isNullOrEmpty() || languageCode.isNullOrEmpty()) {
                    return LoadResult.Page(emptyList(), null, null)
                }

                val wikiSite = WikiSite.forLanguageCode(languageCode)
                var nextKey: Int? = null
                if (prefixSearch) {
                    val response = ServiceFactory.get(wikiSite)
                        .prefixSearch(searchTerm, params.loadSize, params.key)
                    if (response.query?.pages == null) {
                        return LoadResult.Page(emptyList(), null, null)
                    } else {
                        nextKey = response.continuation?.gpsoffset
                    }
                } else {
                    ServiceFactory.get(wikiSite)
                        .fullTextSearchMedia(searchTerm, params.key?.gsroffset?.toString(), params.loadSize, params.key)
                }
                LoadResult.Page(listOf(), null, nextKey)
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }

        override fun getRefreshKey(state: PagingState<Int, SearchResults>): Int? {
            return null
        }
    }

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchResultsViewModel(bundle) as T
        }
    }
}
