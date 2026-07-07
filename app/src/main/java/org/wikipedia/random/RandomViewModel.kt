package org.wikipedia.random

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.random.RandomClient
import org.wikipedia.page.PageTitle
import org.wikipedia.util.Resource
import kotlin.time.Duration.Companion.milliseconds

class RandomViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val wikiSite = savedStateHandle.get<WikiSite>(Constants.ARG_WIKISITE)!!
    val invokeSource = savedStateHandle.get<InvokeSource>(Constants.INTENT_EXTRA_INVOKE_SOURCE) ?: InvokeSource.RANDOM_ACTIVITY

    private val items = mutableStateListOf<PageTitle>()
    private var loading = false

    var loadError by mutableStateOf<Throwable?>(null)
        private set

    private var currentTitle: PageTitle? = null

    var saveButtonState by mutableStateOf(false)
        private set

    init {
        loadMore()
    }

    fun itemAt(page: Int): PageTitle? = items.getOrNull(page)

    fun stateFor(page: Int): Resource<PageTitle> {
        return when {
            page < items.size -> Resource.Success(items[page])
            loadError != null -> Resource.Error(loadError!!)
            else -> Resource.Loading()
        }
    }

    /**
     * Fetch another batch of random articles once the requested page approaches the end of the
     * buffer, so that swiping forward stays ahead of the network.
     */
    fun prefetchIfNeeded(page: Int) {
        if (!loading && loadError == null && page >= items.size - PREFETCH_DISTANCE) {
            loadMore()
        }
    }

    fun retry() {
        if (!loading) {
            loadError = null
            loadMore()
        }
    }

    private fun loadMore() {
        loading = true
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            loading = false
            loadError = throwable
        }) {
            var tries = 0
            while (tries++ < 5) {
                val newItems = RandomClient.getRandomPages(wikiSite, BATCH_SIZE)
                    .filter { !it.thumbUrl.isNullOrEmpty() && !it.extract.isNullOrEmpty() }
                if (!newItems.isEmpty()) {
                    items.addAll(newItems)
                    break
                }
                delay(1000.milliseconds)
            }
            loadError = null
            loading = false
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
        private const val BATCH_SIZE = 10
        private const val PREFETCH_DISTANCE = 5
    }
}
