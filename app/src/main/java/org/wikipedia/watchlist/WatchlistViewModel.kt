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
import org.wikipedia.page.Namespace
import org.wikipedia.settings.Prefs
import java.util.*

class WatchlistViewModel : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = UiState.Error(throwable)
    }

    private var watchlistItems = mutableListOf<MwQueryResult.WatchlistItem>()
    var finalList = mutableListOf<Any>()
    var displayLanguages = WikipediaApp.instance.languageState.appLanguageCodes.filterNot { Prefs.watchlistDisabledLanguages.contains(it) }
    var filterMode = WatchlistFragment.FILTER_MODE_ALL

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    fun updateList() {
        finalList = mutableListOf()
        finalList.add("") // placeholder for header

        val calendar = Calendar.getInstance()
        var curDay = -1

        for (item in watchlistItems) {
            if (filterMode == WatchlistFragment.FILTER_MODE_ALL ||
                (filterMode == WatchlistFragment.FILTER_MODE_PAGES && Namespace.of(item.ns).main()) ||
                (filterMode == WatchlistFragment.FILTER_MODE_TALK && Namespace.of(item.ns).talk()) ||
                (filterMode == WatchlistFragment.FILTER_MODE_OTHER && !Namespace.of(item.ns).main() && !Namespace.of(item.ns).talk())) {

                calendar.time = item.date
                if (calendar.get(Calendar.DAY_OF_YEAR) != curDay) {
                    curDay = calendar.get(Calendar.DAY_OF_YEAR)
                    finalList.add(item.date)
                }

                finalList.add(item)
            }
        }
    }

    fun fetchWatchlist() {
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
            _uiState.value = UiState.Success(watchlistItems)
        }
    }

    open class UiState {
        data class Success(val watchlistItem: List<MwQueryResult.WatchlistItem>) : UiState()
        data class Error(val throwable: Throwable) : UiState()
    }
}
