package org.wikipedia.suggestededits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.settings.Prefs
import java.util.*

class SuggestedEditsImageRecsFragmentViewModel : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = UiState.Error(throwable)
    }

    private var watchlistItems = mutableListOf<MwQueryResult.WatchlistItem>()
    var currentSearchQuery: String? = null
        private set
    var finalList = mutableListOf<Any>()

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        fetchWatchlist()
    }

    fun updateList(searchBarPlaceholder: Boolean = true) {

        finalList = mutableListOf()

        if (searchBarPlaceholder) {
            finalList.add("") // placeholder for search bar
        }

        val calendar = Calendar.getInstance()
        var curDay = -1

        val excludedWikiCodes = Prefs.watchlistExcludedWikiCodes

        watchlistItems.forEach { item ->

            if (excludedWikiCodes.contains(item.wiki?.languageCode)) {
                return@forEach
            }

            val searchQuery = currentSearchQuery
            if (!searchQuery.isNullOrEmpty() &&
                !(item.title.contains(searchQuery, true) ||
                        item.user.contains(searchQuery, true) ||
                        item.parsedComment.contains(searchQuery, true))) {
                return@forEach
            }

            calendar.time = item.date
            if (calendar.get(Calendar.DAY_OF_YEAR) != curDay) {
                curDay = calendar.get(Calendar.DAY_OF_YEAR)
                finalList.add(item.date)
            }

            finalList.add(item)
        }
        _uiState.value = UiState.Success()
    }

    fun fetchWatchlist(searchBarPlaceholder: Boolean = true) {
        _uiState.value = UiState.Loading()
        viewModelScope.launch(handler) {
            watchlistItems = mutableListOf()

            /*
            displayLanguages.map { language ->
                async {
                    withContext(Dispatchers.IO) {
                        ServiceFactory.get(WikiSite.forLanguageCode(language))
                            .getWatchlist(latestRevisions(), showCriteriaString(), showTypesString())
                    }.query?.watchlist?.map {
                        it.wiki = WikiSite.forLanguageCode(language)
                        watchlistItems.add(it)
                    }
                }
            }.awaitAll()
            */

            watchlistItems.sortByDescending { it.date }
            updateList(searchBarPlaceholder)
        }
    }

    open class UiState {
        class Loading : UiState()
        class Success : UiState()
        class Error(val throwable: Throwable) : UiState()
    }
}
