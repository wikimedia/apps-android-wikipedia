package org.wikipedia.talk

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle

class ArchivedTalkPagesViewModel(bundle: Bundle) : ViewModel() {

    val pageTitle = bundle.getParcelable<PageTitle>(Constants.ARG_TITLE)!!
    val archivedTalkPagesFlow = Pager(PagingConfig(pageSize = 10)) {
        ArchivedTalkPagesPagingSource(pageTitle)
    }.flow.cachedIn(viewModelScope)

    class ArchivedTalkPagesPagingSource(
            val pageTitle: PageTitle
    ) : PagingSource<Int, PageTitle>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PageTitle> {
            return try {
                // The default offset is 0 but we send the initial offset from 1 to prevent showing the same talk page from the results.
                if (params.key == 0) {
                    return LoadResult.Page(emptyList(), null, null)
                }
                val response = ServiceFactory.get(WikiSite.forLanguageCode(pageTitle.wikiSite.languageCode))
                    .prefixSearch(pageTitle.prefixedText + "/", params.loadSize, params.key)
                if (response.query?.pages == null) {
                    return LoadResult.Page(emptyList(), null, null)
                }
                val titles = response.query!!.pages!!.map { page ->
                    PageTitle(page.title, pageTitle.wikiSite).also {
                        it.displayText = page.displayTitle(pageTitle.wikiSite.languageCode)
                    }
                }
                LoadResult.Page(titles, null, response.continuation?.gpsoffset)
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
            return ArchivedTalkPagesViewModel(bundle) as T
        }
    }
}
