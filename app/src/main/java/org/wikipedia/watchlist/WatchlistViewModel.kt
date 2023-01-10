package org.wikipedia.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.notifications.NotificationViewModel
import org.wikipedia.settings.Prefs

class WatchlistViewModel : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = NotificationViewModel.UiState.Error(throwable)
    }

    var displayLanguages = WikipediaApp.instance.languageState.appLanguageCodes.filterNot { Prefs.watchlistDisabledLanguages.contains(it) }

    private val _uiState = MutableStateFlow(NotificationViewModel.UiState())
    val uiState = _uiState

    fun fetchWatchlist() {
        viewModelScope.launch(handler) {
                withContext(Dispatchers.IO) {
                    val list = displayLanguages.map {
                        async {
                            ServiceFactory.get(WikiSite.forLanguageCode(it)).getWatchlist()
                        }
                    }.awaitAll().map {
                        val items = ArrayList<MwQueryResult.WatchlistItem>()
                        resultList.forEachIndexed { index, result ->
                            val wiki = WikiSite.forLanguageCode(displayLanguages[index])
                            (result as MwQueryResponse).query?.watchlist?.forEach { item ->
                                item.wiki = wiki
                                items.add(item)
                            }
                        }
                        items
                    }
                }
            }
        }
    }

    open class UiState {
        data class Success(val watchlistItem: List<MwQueryResult.WatchlistItem>) : UiState()
        data class Error(val throwable: Throwable) : UiState()
    }
}
