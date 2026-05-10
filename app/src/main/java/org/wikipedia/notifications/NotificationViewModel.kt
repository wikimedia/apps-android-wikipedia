package org.wikipedia.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.notifications.db.Notification
import org.wikipedia.settings.Prefs
import org.wikipedia.util.Resource
import org.wikipedia.util.StringUtil
import java.util.Date
import java.util.Random

class NotificationViewModel : ViewModel() {

    // repository (local database) for notifications
    private val notificationRepository = NotificationRepository(AppDatabase.instance.notificationDao())
    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = Resource.Error(throwable)
    }

    // in-memory storage of all notifications
    private val notificationList = mutableListOf<Notification>()

    // notification source
    private var dbNameMap = mapOf<String, WikiSite>()

    // stores which tab is active: "All" or "Mentions"
    private var selectedFilterTab: Int = 0
    // pagination token for fetching the next page of notifications
    private var currentContinueStr: String? = null

    // search query established by user
    var currentSearchQuery: String? = null
        private set

    // sum of all unread notifications for the "Mentions"-tab
    var mentionsUnreadCount: Int = 0

    // sum of all unread notification for the "All" tab
    var allUnreadCount: Int = 0

    private val _uiState = MutableStateFlow(Resource<Pair<List<NotificationListItemContainer>, Boolean>>())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch(handler) {
            dbNameMap = notificationRepository.fetchUnreadWikiDbNames()
        }
        fetchAndSave()
    }

    // Reads complete database content to memory, processes it (applying filtering, sorting) and
    // binds it together with a flag calculated from the paging state.
    // Then the uiState is updated triggering UI update.
    private suspend fun filterAndPostNotifications() {
        val pair = Pair(processList(notificationRepository.getAllNotifications()), !currentContinueStr.isNullOrEmpty())
        _uiState.value = Resource.Success(pair)
    }

    // Performs in-memory transformation of the raw data into displayable items.
    // - clearing in-memory list if paging state is not set
    // - iterating through the list in memory and comparing with all additional elements in order to
    // eliminate duplicates (O(n*m))
    // - sorting the in-memory list by date
    // - apply user preference for showing/hiding read entries
    // - apply user preferences for excluding certain types of notifications and certain wikis
    // - apply user-specific search query against title, header, body and secondary link label:
    //   if none of these fields contain the search string (case-insensitive), the notification is
    //   skipped
    // - maintain counters for all unread notifications and all unread notifications of type "Mentions"
    // - apply filtering for "Mentions" is user the corresponding tab is active
    private fun processList(list: List<Notification>): List<NotificationListItemContainer> {
        if (currentContinueStr.isNullOrEmpty()) {
            notificationList.clear()
        }
        for (n in list) {
            if (notificationList.none { it.id == n.id && it.wiki == n.wiki }) {
                notificationList.add(n)
            }
        }
        // Sort them by descending date...
        notificationList.sortByDescending { it.date() }

        // Filtered the tab selection
        val tabSelectedList = notificationList
            .filter { if (Prefs.hideReadNotificationsEnabled) it.isUnread else true }

        val excludedTypeCodes = Prefs.notificationExcludedTypeCodes
        val excludedWikiCodes = Prefs.notificationExcludedWikiCodes
        val includedWikiCodes = NotificationFilterActivity.allWikisList().minus(excludedWikiCodes).map {
            it.split("-")[0]
        }
        val checkExcludedWikiCodes = NotificationFilterActivity.allWikisList().size != includedWikiCodes.size

        val notificationContainerList = mutableListOf<NotificationListItemContainer>()

        allUnreadCount = 0
        mentionsUnreadCount = 0

        // Save into display list
        for (n in tabSelectedList) {
            val linkText = n.contents?.links?.secondary?.firstOrNull()?.label
            val searchQuery = currentSearchQuery
            if (!searchQuery.isNullOrEmpty() &&
                !(n.title?.full?.contains(searchQuery, true) == true ||
                        n.contents?.header?.contains(searchQuery, true) == true ||
                        n.contents?.body?.contains(searchQuery, true) == true ||
                        (linkText?.contains(searchQuery, true) == true))) {
                continue
            }
            if (excludedTypeCodes.find { n.category.startsWith(it) } != null) {
                continue
            }
            if (checkExcludedWikiCodes) {
                val wikiCode = StringUtil.dbNameToLangCode(n.wiki)
                if (!includedWikiCodes.contains(wikiCode)) {
                    continue
                }
            }
            val isMention = NotificationCategory.isMentionsGroup(n.category)
            if (n.isUnread) {
                allUnreadCount++
                if (isMention) {
                    mentionsUnreadCount++
                }
            }
            if (selectedFilterTab == 1 && !isMention) {
                continue
            }
            notificationContainerList.add(NotificationListItemContainer(n))
        }

        return notificationContainerList
    }

    // returns the sum of all excluded wikis plus all excluded type of notifications
    fun excludedFiltersCount(): Int {
        val excludedWikiCodes = Prefs.notificationExcludedWikiCodes
        val excludedTypeCodes = Prefs.notificationExcludedTypeCodes
        return NotificationFilterActivity.allWikisList().count { excludedWikiCodes.contains(it) } +
                NotificationFilterActivity.allTypesIdList().count { excludedTypeCodes.contains(it) }
    }

    // Resets the paging state and erases in-memory notifications if refresh is requested.
    // Checks connectivity state. If device is online, it fetches data from the API and stores it in
    // the database by calling repository function without effective filter and current paging state.
    // UI update is triggered by calling filterAndPostNotifications()
    fun fetchAndSave(refresh: Boolean = false) {
        if (refresh) {
            currentContinueStr = null
            notificationList.clear()
        }

        viewModelScope.launch(handler) {
            if (WikipediaApp.instance.isOnline) {
                currentContinueStr = notificationRepository.fetchAndSave("read|!read", currentContinueStr)
            }
            filterAndPostNotifications()
        }
    }

    // Updates currentSearchQuery and triggers filterAndPostNotifications().
    // Because filtering happens in processList called from filterAndPostNotifications(),
    // the list is immediately re-filtered in memory.
    fun updateSearchQuery(query: String?) {
        currentSearchQuery = query
        viewModelScope.launch(handler) {
            filterAndPostNotifications()
        }
    }

    // update the selection of the tab (between "All" and "Mentions"
    fun updateTabSelection(position: Int) {
        selectedFilterTab = position
        viewModelScope.launch(handler) {
            filterAndPostNotifications()
        }
    }

    // Creates a map of the read notification items keyed by their wiki.
    // This combines multiple read notification in one API call instead of one call per item.
    // After issuing the coroutine for the API call, the function triggers a further coroutine for
    // updating the local database.
    fun markItemsAsRead(items: List<NotificationListItemContainer>, markUnread: Boolean) {
        val notificationsPerWiki = mutableMapOf<WikiSite, MutableList<Notification>>()
        val selectionKey = if (items.size > 1) Random().nextLong() else null
        for (item in items) {
            val notification = item.notification!!
            val wiki = dbNameMap.getOrElse(notification.wiki) {
                when (notification.wiki) {
                    Constants.COMMONS_DB_NAME -> Constants.commonsWikiSite
                    Constants.WIKIDATA_DB_NAME -> Constants.wikidataWikiSite
                    else -> {
                        val langCode = StringUtil.dbNameToLangCode(notification.wiki)
                        WikiSite.forLanguageCode(WikipediaApp.instance.languageState.getDefaultLanguageCode(langCode) ?: langCode)
                    }
                }
            }
            notificationsPerWiki.getOrPut(wiki) { mutableListOf() }.add(notification)
        }

        viewModelScope.launch(handler) {
            for ((wiki, notifications) in notificationsPerWiki) {
                NotificationPollBroadcastReceiver.markRead(wiki, notifications, markUnread)
            }
        }

        // Mark items in read state and save into database
        viewModelScope.launch(handler) {
            notificationList
                .filter { n -> items.map { container -> container.notification?.id }
                .firstOrNull { it == n.id } != null }
                .map {
                    it.read = if (markUnread) null else Date().toString()
                    notificationRepository.updateNotification(it)
                }
            filterAndPostNotifications()
        }
    }
}
