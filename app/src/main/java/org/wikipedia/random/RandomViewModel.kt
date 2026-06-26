package org.wikipedia.random

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.page.PageTitle
import org.wikipedia.util.Resource

class RandomViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val wikiSite = savedStateHandle.get<WikiSite>(Constants.ARG_WIKISITE)!!
    val invokeSource = savedStateHandle.get<InvokeSource>(Constants.INTENT_EXTRA_INVOKE_SOURCE) ?: InvokeSource.RANDOM_ACTIVITY

    val pages = mutableStateMapOf<Int, Resource<PageSummary>>()

    private var currentTitle: PageTitle? = null

    var saveButtonState by mutableStateOf(false)
        private set

    fun loadPage(page: Int, forceReload: Boolean = false) {
        val existing = pages[page]
        if (!forceReload && (existing is Resource.Loading || existing is Resource.Success)) {
            return
        }
        pages[page] = Resource.Loading()
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            pages[page] = Resource.Error(throwable)
        }) {
            pages[page] = Resource.Success(ServiceFactory.getRest(wikiSite).getRandomSummary())
        }
    }

    fun updateSaveState(title: PageTitle?) {
        currentTitle = title
        refreshSaveState()
    }

    fun refreshSaveState() {
        val title = currentTitle
        if (title == null) {
            saveButtonState = false
            return
        }
        viewModelScope.launch(CoroutineExceptionHandler { _, _ -> }) {
            saveButtonState = AppDatabase.instance.readingListPageDao().findPageInAnyList(title) != null
        }
    }

    companion object {
        const val FIRST_PAGE = 0
    }
}
