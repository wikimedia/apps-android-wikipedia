package org.wikipedia.readinglist.recommended

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.RecommendedReadingListEvent
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.settings.Prefs
import org.wikipedia.util.Resource
import org.wikipedia.util.SingleLiveData
import org.wikipedia.util.StringUtil

class RecommendedReadingListInterestsViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val fromSettings = savedStateHandle.get<Boolean>(RecommendedReadingListOnboardingActivity.EXTRA_FROM_SETTINGS) == true

    private val _uiState = MutableStateFlow(Resource<UiState>())
    val uiState: StateFlow<Resource<UiState>> = _uiState.asStateFlow()

    val randomizeEvent = SingleLiveData<UiState>()

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
                    if (i < historyTitles.size && !results.contains(historyTitles[i])) results.add(historyTitles[i])
                    if (i < readingListTitles.size && !results.contains(readingListTitles[i])) results.add(readingListTitles[i])
                }
                // remove non-main namespace articles, or Main page
                results.removeAll { it.isMainPage || it.namespace() != Namespace.MAIN }
            }

            // If there are still VERY few items, include a few random articles.
            if (results.size < 5) {
                for (i in results.size until 5) {
                    val title = ServiceFactory.getRest(WikipediaApp.instance.wikiSite).getRandomSummary()
                        .getPageTitle(WikipediaApp.instance.wikiSite)
                    if (!results.contains(title)) {
                        results.add(title)
                    }
                }
            }

            // Hydrate titles, if necessary
            val itemsNeedingCall = results
                .filter { it.description.isNullOrEmpty() || it.thumbUrl.isNullOrEmpty() }
                .groupBy { it.wikiSite }
            itemsNeedingCall.keys.forEach { site ->
                val pageList = ServiceFactory.get(site).getInfoByPageIdsOrTitles(titles = itemsNeedingCall[site]?.joinToString("|") { it.prefixedText })
                    .query?.pages.orEmpty()
                pageList.forEach { page ->
                    results.find { it.prefixedText == StringUtil.addUnderscores(page.title) }?.let { title ->
                        title.description = page.description
                        title.thumbUrl = page.thumbUrl()
                    }
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

    fun randomizeSelection() {
        (_uiState.value as? Resource.Success<UiState>)?.let {
            val newItems = it.data.items.shuffled()
            val newSelection = newItems.take(3).toSet()
            val newState = UiState(
                fromSettings = it.data.fromSettings,
                fromRandomize = true,
                prevItems = it.data.items,
                prevSelectedItems = it.data.selectedItems,
                items = newItems,
                selectedItems = newSelection)
            _uiState.value = Resource.Success(newState)
            RecommendedReadingListEvent.submit("randomizer_click", "rrl_interests_select")
            randomizeEvent.postValue(newState)
        }
    }

    fun restoreState(items: List<PageTitle>, selectedItems: Set<PageTitle>) {
        (_uiState.value as? Resource.Success<UiState>)?.let {
            val newState = UiState(
                fromSettings = it.data.fromSettings,
                items = items,
                selectedItems = selectedItems
            )
            _uiState.value = Resource.Success(newState)
        }
    }

    fun commitSelection() {
        (_uiState.value as? Resource.Success<UiState>)?.let {
            val selectedItems = it.data.selectedItems.toMutableList()

            if (fromSettings && selectedItems.isEmpty()) {
                selectedItems.add(it.data.items.first())
            }

            Prefs.recommendedReadingListInterests = selectedItems
            if (fromSettings) {
                RecommendedReadingListEvent.submit(
                    action = "interest_select_click",
                    activeInterface = "discover_settings",
                    countSelected = selectedItems.size
                )
            } else {
                RecommendedReadingListEvent.submit(
                    action = "submit_click",
                    activeInterface = "rrl_interests_select",
                    countSelected = selectedItems.size
                )
            }
        }
    }

    fun addTitle(title: PageTitle) {
        (_uiState.value as? Resource.Success<UiState>)?.let {
            val newItems = listOf(title) + it.data.items
            val newSelection = it.data.selectedItems + title
            _uiState.value = Resource.Success(
                it.data.copy(items = newItems, selectedItems = newSelection)
            )
        }
    }

    data class UiState(
        val fromSettings: Boolean = false,
        val items: List<PageTitle> = emptyList(),
        val selectedItems: Set<PageTitle> = emptySet(),
        val fromRandomize: Boolean = false,
        val prevItems: List<PageTitle> = emptyList(),
        val prevSelectedItems: Set<PageTitle> = emptySet()
    )
}
