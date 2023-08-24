package org.wikipedia.suggestededits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.settings.Prefs
import java.util.Calendar

class SuggestedEditsRecentEditsViewModel : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = UiState.Error(throwable)
    }

    private var recentChangesItem = mutableListOf<MwQueryResult.RecentChange>()

    // TODO: implement this
    var displayLanguage = "en"
    var currentSearchQuery: String? = null
        private set
    var finalList = mutableListOf<Any>()

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        fetchRecentEdits()
    }

    fun updateList(searchBarPlaceholder: Boolean = true) {

        finalList = mutableListOf()

        if (searchBarPlaceholder) {
            finalList.add("") // placeholder for search bar
        }

        val calendar = Calendar.getInstance()
        var curDay = -1

        val excludedWikiCodes = Prefs.watchlistExcludedWikiCodes

        recentChangesItem.forEach { item ->

//            if (excludedWikiCodes.contains(item.wiki?.languageCode)) {
//                return@forEach
//            }

            val searchQuery = currentSearchQuery
            if (!searchQuery.isNullOrEmpty() &&
                !(item.title.contains(searchQuery, true) ||
                        item.user.contains(searchQuery, true) ||
                        item.parsedcomment.contains(searchQuery, true))) {
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

    fun fetchRecentEdits(searchBarPlaceholder: Boolean = true) {
        _uiState.value = UiState.Loading()
        viewModelScope.launch(handler) {
            recentChangesItem = mutableListOf()
            // TODO: implement this
//            displayLanguages.map { language ->
//                async {
//                    ServiceFactory.get(WikiSite.forLanguageCode(language))
//                        .getWatchlist(latestRevisions(), showCriteriaString(), showTypesString())
//                        .query?.watchlist?.map {
//                            it.wiki = WikiSite.forLanguageCode(language)
//                            recentChangesItem.add(it)
//                        }
//                }
//            }.awaitAll()
            recentChangesItem.sortByDescending { it.date }
            updateList(searchBarPlaceholder)
        }
    }

    fun updateSearchQuery(query: String?) {
        currentSearchQuery = query
    }

    fun filtersCount(): Int {
        val excludedWikiCodes = Prefs.watchlistExcludedWikiCodes
        val defaultTypeSet = SuggestedEditsRecentEditsFilterTypes.DEFAULT_FILTER_TYPE_SET.map { it.id }.toSet()
        val nonDefaultChangeTypes = Prefs.watchlistIncludedTypeCodes.subtract(defaultTypeSet)
            .union(defaultTypeSet.subtract(Prefs.watchlistIncludedTypeCodes.toSet()))
        return WikipediaApp.instance.languageState.appLanguageCodes.count { excludedWikiCodes.contains(it) } + nonDefaultChangeTypes.size
    }

    private fun latestRevisions(): String? {
        val includedTypesCodes = Prefs.watchlistIncludedTypeCodes
        if (!includedTypesCodes.containsAll(SuggestedEditsRecentEditsFilterTypes.LATEST_REVISIONS_GROUP.map { it.id }) &&
            !includedTypesCodes.contains(SuggestedEditsRecentEditsFilterTypes.LATEST_REVISION.id)) {
            return SuggestedEditsRecentEditsFilterTypes.NOT_LATEST_REVISION.value
        }
        return null
    }

    private fun showCriteriaString(): String {
        val includedTypesCodes = Prefs.watchlistIncludedTypeCodes
        val list = mutableListOf<String>()
        if (!includedTypesCodes.containsAll(SuggestedEditsRecentEditsFilterTypes.UNSEEN_CHANGES_GROUP.map { it.id })) {
            if (includedTypesCodes.contains(SuggestedEditsRecentEditsFilterTypes.UNSEEN_CHANGES.id)) {
                list.add(SuggestedEditsRecentEditsFilterTypes.UNSEEN_CHANGES.value)
            }
            if (includedTypesCodes.contains(SuggestedEditsRecentEditsFilterTypes.SEEN_CHANGES.id)) {
                list.add(SuggestedEditsRecentEditsFilterTypes.SEEN_CHANGES.value)
            }
        }

        if (!includedTypesCodes.containsAll(SuggestedEditsRecentEditsFilterTypes.BOT_EDITS_GROUP.map { it.id })) {
            if (includedTypesCodes.contains(SuggestedEditsRecentEditsFilterTypes.BOT.id)) {
                list.add(SuggestedEditsRecentEditsFilterTypes.BOT.value)
            }
            if (includedTypesCodes.contains(SuggestedEditsRecentEditsFilterTypes.HUMAN.id)) {
                list.add(SuggestedEditsRecentEditsFilterTypes.HUMAN.value)
            }
        }

        if (!includedTypesCodes.containsAll(SuggestedEditsRecentEditsFilterTypes.MINOR_EDITS_GROUP.map { it.id })) {
            if (includedTypesCodes.contains(SuggestedEditsRecentEditsFilterTypes.MINOR_EDITS.id)) {
                list.add(SuggestedEditsRecentEditsFilterTypes.MINOR_EDITS.value)
            }
            if (includedTypesCodes.contains(SuggestedEditsRecentEditsFilterTypes.NON_MINOR_EDITS.id)) {
                list.add(SuggestedEditsRecentEditsFilterTypes.NON_MINOR_EDITS.value)
            }
        }

        if (!includedTypesCodes.containsAll(SuggestedEditsRecentEditsFilterTypes.USER_STATUS_GROUP.map { it.id })) {
            if (includedTypesCodes.contains(SuggestedEditsRecentEditsFilterTypes.REGISTERED.id)) {
                list.add(SuggestedEditsRecentEditsFilterTypes.REGISTERED.value)
            }
            if (includedTypesCodes.contains(SuggestedEditsRecentEditsFilterTypes.UNREGISTERED.id)) {
                list.add(SuggestedEditsRecentEditsFilterTypes.UNREGISTERED.value)
            }
        }
        return list.joinToString(separator = "|")
    }

    private fun showTypesString(): String {
        val includedTypesCodes = Prefs.watchlistIncludedTypeCodes
        val types = SuggestedEditsRecentEditsFilterTypes.TYPE_OF_CHANGES_GROUP.filter { includedTypesCodes.contains(it.id) }.map { it.value }
        return types.joinToString(separator = "|")
    }

    open class UiState {
        class Loading : UiState()
        class Success : UiState()
        class Error(val throwable: Throwable) : UiState()
    }
}
