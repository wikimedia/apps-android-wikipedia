package org.wikipedia.edit.insertmedia

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.page.PageTitle

class InsertMediaViewModel(bundle: Bundle) : ViewModel() {

    var searchQuery = bundle.getString(InsertMediaActivity.EXTRA_SEARCH_QUERY)!!
    var selectedImage: MediaSearchResult? = null
    val insertMediaFlow = Pager(PagingConfig(pageSize = 10)) {
        InsertMediaPagingSource(searchQuery)
    }.flow.cachedIn(viewModelScope)

    class InsertMediaPagingSource(
        val searchQuery: String,
    ) : PagingSource<MwQueryResponse.Continuation, MediaSearchResult>() {
        override suspend fun load(params: LoadParams<MwQueryResponse.Continuation>): LoadResult<MwQueryResponse.Continuation, MediaSearchResult> {
            return try {
                val wikiSite = WikiSite(Service.COMMONS_URL)
                val response = ServiceFactory.get(wikiSite)
                    .fullTextSearchMedia("File: $searchQuery", params.key?.gsroffset?.toString(), params.loadSize, params.key?.continuation)

                return response.query?.pages?.let { list ->
                    val results = list.sortedBy { it.index }.map {
                        val pageTitle = PageTitle(it.title, wikiSite, it.imageInfo()?.thumbUrl)
                        MediaSearchResult(pageTitle, it.imageInfo())
                    }
                    LoadResult.Page(results, null, response.continuation)
                } ?: run {
                    LoadResult.Page(emptyList(), null, null)
                }
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }

        override fun getRefreshKey(state: PagingState<MwQueryResponse.Continuation, MediaSearchResult>): MwQueryResponse.Continuation? {
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
