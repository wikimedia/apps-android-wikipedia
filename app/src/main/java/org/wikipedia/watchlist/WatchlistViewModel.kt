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
        val includedTypesCodes = Prefs.watchlistIncludedTypeCodes

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

            if (!includedTypesCodes.containsAll(WatchlistFilterTypes.UNSEEN_CHANGES_GROUP.map { it.id })) {
                if (includedTypesCodes.contains(WatchlistFilterTypes.UNSEEN_CHANGES.id) && item.notificationTimestamp.isEmpty()) {
                    return@forEach
                }
                if (includedTypesCodes.contains(WatchlistFilterTypes.SEEN_CHANGES.id) && item.notificationTimestamp.isNotEmpty()) {
                    return@forEach
                }
            }

            if (!includedTypesCodes.containsAll(WatchlistFilterTypes.BOT_EDITS_GROUP.map { it.id })) {
                if (includedTypesCodes.contains(WatchlistFilterTypes.BOT.id) && !item.isBot) {
                    return@forEach
                }
                if (includedTypesCodes.contains(WatchlistFilterTypes.HUMAN.id) && item.isBot) {
                    return@forEach
                }
            }

            if (!includedTypesCodes.containsAll(WatchlistFilterTypes.MINOR_EDITS_GROUP.map { it.id })) {
                if (includedTypesCodes.contains(WatchlistFilterTypes.MINOR_EDITS.id) && !item.isMinor) {
                    return@forEach
                }
                if (includedTypesCodes.contains(WatchlistFilterTypes.NON_MINOR_EDITS.id) && item.isMinor) {
                    return@forEach
                }
            }

            if (includedTypesCodes.any { WatchlistFilterTypes.TYPE_OF_CHANGES_GROUP.any { item -> it == item.id } }) {
                val matched = WatchlistFilterTypes.TYPE_OF_CHANGES_GROUP.any { includedTypesCodes.contains(it.id) && item.type == it.value }
                if (!matched) {
                    return@forEach
                }
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
            updateList(searchBarPlaceholder)
        }
    }

    fun updateSearchQuery(query: String?) {
        currentSearchQuery = query
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
