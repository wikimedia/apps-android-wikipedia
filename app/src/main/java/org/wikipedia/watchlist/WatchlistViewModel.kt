package org.wikipedia.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    var filterMode = WatchlistFragment.FILTER_MODE_ALL
    var displayLanguages = WikipediaApp.instance.languageState.appLanguageCodes.filterNot { Prefs.watchlistDisabledLanguages.contains(it) }

    private val watchlistItems = mutableListOf<MwQueryResult.WatchlistItem>()
    val finalList = mutableListOf<Any>()

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState

    fun updateList() {
        finalList.clear()
        finalList.add("") // placeholder for header

        val calendar = Calendar.getInstance()
        var curDay = -1

        for (item in watchlistItems) {
            if ((filterMode == WatchlistFragment.FILTER_MODE_ALL) ||
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
        _uiState.value = UiState.Success(finalList)
    }

    fun fetchWatchlist() {
        viewModelScope.launch(handler) {
            watchlistItems.clear()
            displayLanguages.map { language ->
                withContext(Dispatchers.Default) {
                    ServiceFactory.get(WikiSite.forLanguageCode(language)).getWatchlist()
                }.query?.watchlist?.map {
                    it.wiki = WikiSite.forLanguageCode(language)
                    watchlistItems.add(it)
                }
            }

            watchlistItems.sortByDescending { it.date }

            updateList()
        }
    }

    open class UiState {
        data class Success(val watchlistItem: List<Any>) : UiState()
        data class Error(val throwable: Throwable) : UiState()
    }
}
