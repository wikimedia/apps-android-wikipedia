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
import org.wikipedia.settings.Prefs

class WatchlistViewModel : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = UiState.Error(throwable)
    }

    var displayLanguages = WikipediaApp.instance.languageState.appLanguageCodes.filterNot { Prefs.watchlistDisabledLanguages.contains(it) }
    var filterMode = WatchlistFragment.FILTER_MODE_ALL

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState

    fun fetchWatchlist() {
        viewModelScope.launch(handler) {
            val list = mutableListOf<MwQueryResult.WatchlistItem>()
            displayLanguages.map { language ->
                withContext(Dispatchers.Default) {
                    ServiceFactory.get(WikiSite.forLanguageCode(language)).getWatchlist()
                }.query?.watchlist?.map {
                    it.wiki = WikiSite.forLanguageCode(language)
                    list.add(it)
                }
            }
            _uiState.value = UiState.Success(list)
        }
    }

    open class UiState {
        data class Success(val watchlistItem: List<MwQueryResult.WatchlistItem>) : UiState()
        data class Error(val throwable: Throwable) : UiState()
    }
}
