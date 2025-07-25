package org.wikipedia.watchlist

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.WatchlistAnalyticsHelper
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.StringUtil
import java.io.IOException
import java.util.Calendar

class WatchlistViewModel : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = Resource.Error(throwable)
    }

    private var watchlistItems = mutableListOf<MwQueryResult.WatchlistItem>()
    var currentSearchQuery: String? = null
        private set
    var finalList = mutableListOf<Any>()
    var displayLanguages = WikipediaApp.instance.languageState.appLanguageCodes.filterNot { Prefs.watchlistExcludedWikiCodes.contains(it) }

    private val _uiState = MutableStateFlow(Resource<Unit>())
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
        _uiState.value = Resource.Success(Unit)
    }

    fun fetchWatchlist(searchBarPlaceholder: Boolean = true) {
        _uiState.value = Resource.Loading()
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

        val findSelectedTypesOfChanges = Prefs.watchlistIncludedTypeCodes
            .filter { code -> WatchlistFilterTypes.TYPE_OF_CHANGES_GROUP.map { it.id }.contains(code) }

        // It should include: "not" default values + "non-selected" default values
        val defaultTypeOfChangeSet = WatchlistFilterTypes.DEFAULT_FILTER_TYPE_OF_CHANGES.map { it.id }.toSet()
        val nonDefaultChangeTypes = findSelectedTypesOfChanges.subtract(defaultTypeOfChangeSet)
            .union(defaultTypeOfChangeSet.subtract(findSelectedTypesOfChanges.toSet()))

        // Find the remaining selected filters
        val findSelectedOthers = Prefs.watchlistIncludedTypeCodes.subtract(findSelectedTypesOfChanges.toSet())
        val defaultOthersSet = WatchlistFilterTypes.DEFAULT_FILTER_OTHERS.map { it.id }.toSet()
        val nonDefaultOthers = defaultOthersSet.subtract(findSelectedOthers)

        return WikipediaApp.instance.languageState.appLanguageCodes.count { excludedWikiCodes.contains(it) } + nonDefaultChangeTypes.size + nonDefaultOthers.size
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

    companion object {
        suspend fun watchPageTitle(scope: CoroutineScope, pageTitle: PageTitle, unwatch: Boolean, expiry: WatchlistExpiry = WatchlistExpiry.NEVER,
                                   isCurrentlyWatched: Boolean? = null, isTalkPage: Boolean? = null): Pair<Boolean, String> {
            isCurrentlyWatched?.let {
                if (it) {
                    WatchlistAnalyticsHelper.logRemovedFromWatchlist(pageTitle)
                } else {
                    WatchlistAnalyticsHelper.logAddedToWatchlist(pageTitle)
                }
            }

            var whichMessage = if (unwatch) {
                "removedwatchtext"
            } else {
                if (expiry == WatchlistExpiry.NEVER) {
                    "addedwatchindefinitelytext"
                } else {
                    "addedwatchexpirytext"
                }
            }
            if (isTalkPage == true) {
                whichMessage += "-talk"
            }

            val watchCall = scope.async {
                val token = ServiceFactory.get(pageTitle.wikiSite).getWatchToken().query?.watchToken()
                ServiceFactory.get(pageTitle.wikiSite)
                    .watch(if (unwatch) 1 else null, null, pageTitle.prefixedText, expiry.expiry, token!!)
            }
            val messageCall = scope.async {
                val unparsedMessage = ServiceFactory.get(pageTitle.wikiSite).getMessages(whichMessage, "${StringUtil.removeUnderscores(pageTitle.prefixedText)}|${L10nUtil.getString(expiry.stringId)}",
                    WikipediaApp.instance.appOrSystemLanguageCode)
                    .query?.allmessages?.firstOrNull { it.name == whichMessage }?.content.orEmpty()
                ServiceFactory.get(pageTitle.wikiSite).parseText(unparsedMessage)
            }

            val watchObj = watchCall.await().getFirst()
            if (watchObj == null) {
                throw IOException("Watch response is null.")
            }
            val message = StringUtil.fromHtml(messageCall.await().text).toString().trim()

            if (unwatch) {
                WatchlistAnalyticsHelper.logRemovedFromWatchlistSuccess(pageTitle)
            } else {
                WatchlistAnalyticsHelper.logAddedToWatchlistSuccess(pageTitle)
            }
            return watchObj.watched to message
        }

        fun showWatchlistSnackbar(activity: AppCompatActivity, fragmentManager: FragmentManager, pageTitle: PageTitle, isWatched: Boolean, message: String) {
            if (!isWatched) {
                FeedbackUtil.showMessage(activity, message)
            } else {
                FeedbackUtil.makeSnackbar(activity, message)
                    .setAction(R.string.watchlist_page_add_to_watchlist_snackbar_action) {
                        ExclusiveBottomSheetPresenter.show(fragmentManager, WatchlistExpiryDialog.newInstance(pageTitle))
                    }
                    .show()
            }
        }
    }
}
