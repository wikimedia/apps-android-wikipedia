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
    var displayLanguages = WikipediaApp.instance.languageState.appLanguageCodes.filterNot { Prefs.watchlistExcludedWikiCodes.contains(it) }

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
            displayLanguages.map { language ->
                async {
                    ServiceFactory.get(WikiSite.forLanguageCode(language))
                        .getWatchlist(latestRevisions(), showCriteriaString(), showTypesString())
                        .query?.watchlist?.map {
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

    fun filtersCount(): Int {
        val excludedWikiCodes = Prefs.watchlistExcludedWikiCodes
        val defaultTypeSet = WatchlistFilterTypes.DEFAULT_FILTER_TYPE_SET.map { it.id }.toSet()
        val nonDefaultChangeTypes = Prefs.watchlistIncludedTypeCodes.subtract(defaultTypeSet)
            .union(defaultTypeSet.subtract(Prefs.watchlistIncludedTypeCodes.toSet()))
        return WikipediaApp.instance.languageState.appLanguageCodes.count { excludedWikiCodes.contains(it) } + nonDefaultChangeTypes.size
    }

    private fun latestRevisions(): String? {
        val includedTypesCodes = Prefs.watchlistIncludedTypeCodes
        if (!includedTypesCodes.containsAll(WatchlistFilterTypes.LATEST_REVISIONS_GROUP.map { it.id }) &&
            !includedTypesCodes.contains(WatchlistFilterTypes.LATEST_REVISION.id)) {
            return WatchlistFilterTypes.NOT_LATEST_REVISION.value
        }
        return null
    }

    private fun showCriteriaString(): String {
        val includedTypesCodes = Prefs.watchlistIncludedTypeCodes
        val list = mutableListOf<String>()
        if (!includedTypesCodes.containsAll(WatchlistFilterTypes.UNSEEN_CHANGES_GROUP.map { it.id })) {
            if (includedTypesCodes.contains(WatchlistFilterTypes.UNSEEN_CHANGES.id)) {
                list.add(WatchlistFilterTypes.UNSEEN_CHANGES.value)
            }
            if (includedTypesCodes.contains(WatchlistFilterTypes.SEEN_CHANGES.id)) {
                list.add(WatchlistFilterTypes.SEEN_CHANGES.value)
            }
        }

        if (!includedTypesCodes.containsAll(WatchlistFilterTypes.BOT_EDITS_GROUP.map { it.id })) {
            if (includedTypesCodes.contains(WatchlistFilterTypes.BOT.id)) {
                list.add(WatchlistFilterTypes.BOT.value)
            }
            if (includedTypesCodes.contains(WatchlistFilterTypes.HUMAN.id)) {
                list.add(WatchlistFilterTypes.HUMAN.value)
            }
        }

        if (!includedTypesCodes.containsAll(WatchlistFilterTypes.MINOR_EDITS_GROUP.map { it.id })) {
            if (includedTypesCodes.contains(WatchlistFilterTypes.MINOR_EDITS.id)) {
                list.add(WatchlistFilterTypes.MINOR_EDITS.value)
            }
            if (includedTypesCodes.contains(WatchlistFilterTypes.NON_MINOR_EDITS.id)) {
                list.add(WatchlistFilterTypes.NON_MINOR_EDITS.value)
            }
        }

        if (!includedTypesCodes.containsAll(WatchlistFilterTypes.USER_STATUS_GROUP.map { it.id })) {
            if (includedTypesCodes.contains(WatchlistFilterTypes.REGISTERED.id)) {
                list.add(WatchlistFilterTypes.REGISTERED.value)
            }
            if (includedTypesCodes.contains(WatchlistFilterTypes.UNREGISTERED.id)) {
                list.add(WatchlistFilterTypes.UNREGISTERED.value)
            }
        }
        return list.joinToString(separator = "|")
    }

    private fun showTypesString(): String {
        val includedTypesCodes = Prefs.watchlistIncludedTypeCodes
        val types = WatchlistFilterTypes.TYPE_OF_CHANGES_GROUP.filter { includedTypesCodes.contains(it.id) }.map { it.value }
        return types.joinToString(separator = "|")
    }

    open class UiState {
        class Loading : UiState()
        class Success : UiState()
        class Error(val throwable: Throwable) : UiState()
    }
}
