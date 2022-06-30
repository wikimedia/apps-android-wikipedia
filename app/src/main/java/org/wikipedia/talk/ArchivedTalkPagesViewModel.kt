package org.wikipedia.talk

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.page.PageTitle

class ArchivedTalkPagesViewModel(bundle: Bundle) : ViewModel() {

    val pageTitle = bundle.getParcelable<PageTitle>(ArchivedTalkPagesActivity.EXTRA_TITLE)!!

    val archivedTalkPagesFlow = Pager(PagingConfig(pageSize = 10)) {
        ArchivedTalkPagesPagingSource(pageTitle)
    }.flow.cachedIn(viewModelScope)

    class ArchivedTalkPagesPagingSource(
            val pageTitle: PageTitle
    ) : PagingSource<MwQueryResponse.Continuation, PageTitle>() {
        override suspend fun load(params: LoadParams<MwQueryResponse.Continuation>): LoadResult<MwQueryResponse.Continuation, PageTitle> {
            return try {
                val response = ServiceFactory.get(WikiSite.forLanguageCode(pageTitle.wikiSite.languageCode))
                        .searchSubPages(pageTitle.prefixedText, params.loadSize, params.key?.gcmContinuation, params.key?.gsroffset.toString())
                if (response.query == null) {
                    return LoadResult.Page(emptyList(), null, null)
                }
                val titles = response.query!!.pages!!.map { page ->
                    PageTitle(page.title, pageTitle.wikiSite).also {
                        it.description = page.description.orEmpty()
                        it.thumbUrl = page.thumbUrl()
                        it.displayText = page.displayTitle(pageTitle.wikiSite.languageCode)
                    }
                }
                LoadResult.Page(titles, null, response.continuation)
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }

        override fun getRefreshKey(state: PagingState<MwQueryResponse.Continuation, PageTitle>): MwQueryResponse.Continuation? {
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
