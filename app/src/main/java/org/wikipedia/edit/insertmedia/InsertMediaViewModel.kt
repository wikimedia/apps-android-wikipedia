package org.wikipedia.edit.insertmedia

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.page.PageTitle
import org.wikipedia.search.SearchResult
import org.wikipedia.search.SearchResults

class InsertMediaViewModel(bundle: Bundle) : ViewModel() {

    val pageTitle = bundle.getParcelable<PageTitle>(InsertMediaActivity.EXTRA_TITLE)!!
    val searchQuery = bundle.getString(InsertMediaActivity.EXTRA_SEARCH_QUERY)!!
    val insertMediaFlow = Pager(PagingConfig(pageSize = 10)) {
        InsertMediaPagingSource(pageTitle, searchQuery)
    }.flow.cachedIn(viewModelScope)

    class InsertMediaPagingSource(
            val pageTitle: PageTitle,
            val searchQuery: String,
    ) : PagingSource<MwQueryResponse.Continuation, SearchResult>() {
        override suspend fun load(params: LoadParams<MwQueryResponse.Continuation>): LoadResult<MwQueryResponse.Continuation, SearchResult> {
            return try {
                val response = ServiceFactory.get(WikiSite.forLanguageCode(pageTitle.wikiSite.languageCode))
                    .fullTextSearch(searchQuery, params.key?.gsroffset.toString(), params.loadSize, params.key?.continuation)
                if (response.query?.pages == null) {
                    return LoadResult.Page(emptyList(), null, null)
                }

                return response.query?.pages?.let { list ->
                    val results = list.sortedBy { it.index }.map { SearchResult(it, pageTitle.wikiSite) }
                    LoadResult.Page(results, response.continuation, null)
                } ?: run {
                    LoadResult.Page(emptyList(), null, null)
                }
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }

        override fun getRefreshKey(state: PagingState<MwQueryResponse.Continuation, SearchResult>): MwQueryResponse.Continuation? {
            return null
        }
    }

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InsertMediaViewModel(bundle) as T
        }
    }
}
