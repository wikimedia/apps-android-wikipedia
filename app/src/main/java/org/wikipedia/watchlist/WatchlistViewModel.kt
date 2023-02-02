package org.wikipedia.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.settings.Prefs
import java.util.*

class WatchlistViewModel : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = UiState.Error(throwable)
    }

    private var watchlistItems = mutableListOf<MwQueryResult.WatchlistItem>()
    var currentSearchQuery: String? = null
        private set
    var finalList = mutableListOf<Any>()
    var displayLanguages = WikipediaApp.instance.languageState.appLanguageCodes.filterNot { Prefs.watchlistDisabledLanguages.contains(it) }

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
        val list = watchlistItems.filterNot { excludedWikiCodes.contains(it.wiki?.languageCode)  }

        for (item in list) {
            calendar.time = item.date
            if (calendar.get(Calendar.DAY_OF_YEAR) != curDay) {
                curDay = calendar.get(Calendar.DAY_OF_YEAR)
                finalList.add(item.date)
            }

            finalList.add(item)
        }
    }

    fun fetchWatchlist() {
        _uiState.value = UiState.Loading()
        viewModelScope.launch(handler) {
            watchlistItems = mutableListOf()
            displayLanguages.map { language ->
                async {
                    withContext(Dispatchers.IO) {
                        ServiceFactory.get(WikiSite.forLanguageCode(language)).getWatchlist()
                    }.query?.watchlist?.map {
                        it.wiki = WikiSite.forLanguageCode(language)
                        watchlistItems.add(it)
                    }
                }
            }.awaitAll()
            watchlistItems.sortByDescending { it.date }
            _uiState.value = UiState.Success()
        }
    }

    fun updateSearchQuery(query: String?) {
        currentSearchQuery = query
        fetchWatchlist()
    }

    fun excludedFiltersCount(): Int {
        val excludedWikiCodes = Prefs.watchlistExcludedWikiCodes
        val includedTypesCodes = Prefs.watchlistIncludedTypeCodes
        return WikipediaApp.instance.languageState.appLanguageCodes.count { excludedWikiCodes.contains(it) } + includedTypesCodes.size
    }

    open class UiState {
        class Loading : UiState()
        class Success : UiState()
        class Error(val throwable: Throwable) : UiState()
    }
}
