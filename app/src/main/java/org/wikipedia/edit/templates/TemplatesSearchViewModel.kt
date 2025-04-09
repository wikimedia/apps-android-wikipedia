package org.wikipedia.edit.templates

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.TemplateDataResponse
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs

class TemplatesSearchViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val invokeSource = savedStateHandle.get<Constants.InvokeSource>(Constants.INTENT_EXTRA_INVOKE_SOURCE)!!
    val wikiSite = savedStateHandle.get<WikiSite>(Constants.ARG_WIKISITE)!!
    val isFromDiff = savedStateHandle[TemplatesSearchActivity.EXTRA_FROM_DIFF] ?: false
    var searchQuery: String? = null
    var selectedPageTitle: PageTitle? = null
    val searchTemplatesFlow = Pager(PagingConfig(pageSize = 10)) {
        SearchTemplatesFlowSource(searchQuery, wikiSite)
    }.flow.cachedIn(viewModelScope)

    val uiState = MutableStateFlow(UiState())

    fun loadTemplateData(pageTitle: PageTitle) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            uiState.value = UiState.LoadError(throwable)
        }) {
            val response = ServiceFactory.get(pageTitle.wikiSite).getTemplateData(pageTitle.wikiSite.languageCode, pageTitle.prefixedText)
            uiState.value = UiState.LoadTemplateData(pageTitle, response.getTemplateData.first())
        }
    }

    class SearchTemplatesFlowSource(val searchQuery: String?, val wikiSite: WikiSite) : PagingSource<Int, PageTitle>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PageTitle> {
            return try {
                if (searchQuery.isNullOrEmpty()) {
                    val recentUsedTemplates = Prefs.recentUsedTemplates.filter { it.wikiSite == wikiSite }
                    return LoadResult.Page(recentUsedTemplates, null, null)
                }
                val query = Namespace.TEMPLATE.name + ":" + searchQuery
                val response = ServiceFactory.get(wikiSite)
                    .fullTextSearchTemplates("$query*", params.loadSize, params.key)

                return response.query?.pages?.let { list ->
                    val partition = list.partition { it.title.equals(query, true) }.apply {
                        second.sortedBy { it.index }
                    }
                    val results = partition.toList().flatten().map {
                        val pageTitle = PageTitle(wikiSite = wikiSite, _text = it.title, description = it.description)
                        pageTitle.displayText = it.displayTitle(wikiSite.languageCode)
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

    open class UiState {
        data class LoadTemplateData(val pageTitle: PageTitle, val templateData: TemplateDataResponse.TemplateData) : UiState()
        data class LoadError(val throwable: Throwable) : UiState()
    }
}
