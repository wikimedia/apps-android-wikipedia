package org.wikipedia.search

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle

class SearchResultsViewModel(bundle: Bundle) : ViewModel() {

    var searchTerm: String? = null
    var languageCode: String? = null
    val searchResultsFlow = Pager(PagingConfig(pageSize = 20)) {
        SearchResultsPagingSource(searchTerm, languageCode)
    }.flow.cachedIn(viewModelScope)

    class SearchResultsPagingSource(
            val searchTerm: String?,
            val languageCode: String?
    ) : PagingSource<Int, SearchResults>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchResults> {
            return try {
                // TODO: add delay logic
                // TODO: remove `continuation` from SearchResults
                // TODO: remove `suggestion` from SearchResults since the `generator=prefixsearch` will not output the suggestion
                // The default offset is 0 but we send the initial offset from 1 to prevent showing the same talk page from the results.
                if (searchTerm.isNullOrEmpty() || languageCode.isNullOrEmpty()) {
                    return LoadResult.Page(emptyList(), null, null)
                }
                val response = ServiceFactory.get(WikiSite.forLanguageCode(languageCode))
                    .prefixSearch(searchTerm, params.loadSize, params.key)
                if (response.query?.pages == null) {
                    return LoadResult.Page(emptyList(), null, null)
                }
                val titles = response.query!!.pages!!.map { page ->
                    PageTitle(page.title, pageTitle.wikiSite).also {
                        it.displayText = page.displayTitle(pageTitle.wikiSite.languageCode)
                    }
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
