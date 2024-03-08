package org.wikipedia.edit.templates

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.parcelable
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle

class TemplatesSearchViewModel(bundle: Bundle) : ViewModel() {

    val invokeSource = bundle.getSerializable(Constants.INTENT_EXTRA_INVOKE_SOURCE) as Constants.InvokeSource
    val wikiSite = bundle.parcelable<WikiSite>(Constants.ARG_WIKISITE)!!

    val searchTemplatesFlow = Pager(PagingConfig(pageSize = 10)) {
        SearchTemplatesFlowSource("", wikiSite)
    }.flow.cachedIn(viewModelScope)

    class SearchTemplatesFlowSource(val searchQuery: String, val wikiSite: WikiSite) : PagingSource<Int, PageTitle>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PageTitle> {
            return try {
                // TODO: check if the description is valid
                val query = Namespace.TEMPLATE.name + ":" + searchQuery
                val response = ServiceFactory.get(wikiSite)
                    .fullTextSearchTemplates(query, params.loadSize, params.key)

                return response.query?.pages?.let { list ->
                    val results = list.sortedBy { it.index }.map {
                        val pageTitle = PageTitle(it.title, wikiSite, description = it.description)
                        pageTitle
                    }
                    LoadResult.Page(results, null, response.continuation?.gsroffset)
                } ?: run {
                    LoadResult.Page(emptyList(), null, null)
                }
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }

        override fun getRefreshKey(state: PagingState<Int, PageTitle>): Int? {
            return null
        }
    }

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TemplatesSearchViewModel(bundle) as T
        }
    }
}
