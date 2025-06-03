package org.wikipedia.readinglist.recommended

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
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
import org.wikipedia.settings.Prefs
import org.wikipedia.util.Resource
import org.wikipedia.util.SingleLiveData

class RecommendedReadingListInterestsViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val fromSettings = savedStateHandle.get<Boolean>(Constants.ARG_BOOLEAN) == true

    private val _uiState = MutableStateFlow(Resource<UiState>())
    val uiState: StateFlow<Resource<UiState>> = _uiState.asStateFlow()

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

            // start with the user's existing interests
            val selectedItems = Prefs.recommendedReadingListInterests
            results.addAll(selectedItems)

            if (results.size < maxItems) {
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
            }

            // If there are still VERY few items, include a few random articles.
            if (results.size < 5) {
                for (i in results.size until 5) {
                    results.add(ServiceFactory.getRest(WikipediaApp.instance.wikiSite).getRandomSummary()
                        .getPageTitle(WikipediaApp.instance.wikiSite))
                }
            }

            _uiState.value = Resource.Success(
                UiState(
                    fromSettings = fromSettings,
                    items = results,
                    selectedItems = selectedItems.toSet()
                )
            )
        }
    }

    fun toggleSelection(item: PageTitle) {
        (_uiState.value as? Resource.Success<UiState>)?.let {
            val newSelection = if (it.data.selectedItems.contains(item)) {
                it.data.selectedItems - item
            } else {
                it.data.selectedItems + item
            }
            _uiState.value = Resource.Success(
                it.data.copy(selectedItems = newSelection)
            )
        }
    }

    data class UiState(
        val fromSettings: Boolean = false,
        val items: List<PageTitle> = emptyList(),
        val selectedItems: Set<PageTitle> = emptySet()
    )
}
