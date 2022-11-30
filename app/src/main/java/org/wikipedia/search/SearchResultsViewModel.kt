package org.wikipedia.search

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite

class SearchResultsViewModel(bundle: Bundle) : ViewModel() {

    private val BATCH_SIZE = 20
    var searchTerm: String? = null
    var languageCode: String? = null
    val searchResultsFlow = Pager(PagingConfig(pageSize = BATCH_SIZE)) {
        SearchResultsPagingSource(searchTerm, languageCode)
    }.flow.map { pagingData ->
        if (searchTerm.isNullOrEmpty() || languageCode.isNullOrEmpty()) {
            pagingData
        } else {
            val searchQuery = searchTerm!!
            val searchLanguageCode = languageCode!!

            // TODO: is this necessary or need to call before pulling full text search?
            val prefixSearch = withContext(Dispatchers.IO) {
                async {
                    ServiceFactory.get(WikiSite.forLanguageCode(searchLanguageCode))
                        .prefixSearch(searchQuery, BATCH_SIZE, 0)
                }
            }

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

            val searchResults = prefixSearch.await().query?.pages?.let {
                SearchResults(it, WikiSite.forLanguageCode(searchLanguageCode))
            } ?: SearchResults()

            pagingData
                .insertHeaderItem(item = searchResults)
                .insertHeaderItem(item = readingListSearch)
                .insertHeaderItem(item = historySearch)
        }
    }.cachedIn(viewModelScope)

    class SearchResultsPagingSource(
            val searchTerm: String?,
            val languageCode: String?
    ) : PagingSource<Int, SearchResults>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchResults> {
            return try {
                // TODO: add delay logic
                // The default offset is 0 but we send the initial offset from 1 to prevent showing the same talk page from the results.
                if (searchTerm.isNullOrEmpty() || languageCode.isNullOrEmpty()) {
                    return LoadResult.Page(emptyList(), null, null)
                }

                val response = ServiceFactory.get(WikiSite.forLanguageCode(languageCode))
                    .prefixSearch(searchTerm, params.loadSize, params.key)
                if (response.query?.pages == null) {
                    return LoadResult.Page(emptyList(), null, null)
                }
                LoadResult.Page(listOf(), null, response.continuation?.gpsoffset)
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
