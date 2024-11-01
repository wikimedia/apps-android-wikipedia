package org.wikipedia.categories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle

class CategoryActivityViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val pageTitle = savedStateHandle.get<PageTitle>(Constants.ARG_TITLE)!!
    var showSubcategories = false

    val categoryMembersFlow = Pager(PagingConfig(pageSize = 10)) {
        CategoryMembersPagingSource("page")
    }.flow.cachedIn(viewModelScope)

    val subcategoriesFlow = Pager(PagingConfig(pageSize = 10)) {
        CategoryMembersPagingSource("subcat")
    }.flow.cachedIn(viewModelScope)

    inner class CategoryMembersPagingSource(
        private val resultType: String
    ) : PagingSource<String, PageTitle>() {
        override suspend fun load(params: LoadParams<String>): LoadResult<String, PageTitle> {
            return try {
                val response = ServiceFactory.get(WikiSite.forLanguageCode(pageTitle.wikiSite.languageCode))
                        .getCategoryMembers(pageTitle.prefixedText, resultType, params.loadSize, params.key)
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
                LoadResult.Page(titles, null, response.continuation?.gcmContinuation)
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }

        override fun getRefreshKey(state: PagingState<String, PageTitle>): String? {
            return null
        }
    }
}
