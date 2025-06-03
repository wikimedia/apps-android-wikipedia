package org.wikipedia.readinglist.recommended

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.Resource
import org.wikipedia.util.SingleLiveData

class RecommendedReadingListInterestsViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val fromSettings = savedStateHandle.get<Boolean>(Constants.ARG_BOOLEAN) == true

    private val _uiState = MutableStateFlow(Resource<UiState>())
    val uiState: StateFlow<Resource<UiState>> = _uiState.asStateFlow()

    val historyItems = MutableLiveData(Resource<List<Any>>())

    init {
        loadItems()
    }

    fun loadItems() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _uiState.value = Resource.Error(throwable)
        }) {
            _uiState.value = Resource.Loading()

            val maxItems = 20
            val results = mutableListOf<PageTitle>()
            // get most recent history entries
            val historyTitles = AppDatabase.instance.historyEntryWithImageDao().findEntryForReadMore(maxItems, 0)
                .map { it.title }
            // and a random sampling of reading list pages
            val readingListTitles = AppDatabase.instance.readingListPageDao().getPagesByRandom(maxItems)
                .map { ReadingListPage.toPageTitle(it) }
            // take the two lists and interleave them
            for (i in 0 until maxItems) {
                if (i < historyTitles.size) results.add(historyTitles[i])
                if (i < readingListTitles.size) results.add(readingListTitles[i])
            }

            // If there are VERY few items, include some random articles.
            if (results.size < 5) {
                for (i in results.size until 5) {
                    results.add(ServiceFactory.getRest(WikipediaApp.instance.wikiSite).getRandomSummary()
                        .getPageTitle(WikipediaApp.instance.wikiSite))
                }
            }

            _uiState.value = Resource.Success(
                UiState(
                    fromSettings = fromSettings,
                    items = results
                )
            )
        }
    }

    data class UiState(
        val fromSettings: Boolean = false,
        val items: List<PageTitle> = emptyList()
    )
}
