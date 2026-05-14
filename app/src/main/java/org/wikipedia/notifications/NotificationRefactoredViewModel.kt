package org.wikipedia.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.notifications.db.Notification
import org.wikipedia.util.Resource
import org.wikipedia.util.StringUtil
import java.util.Random

class NotificationRefactoredViewModel(
    private val notificationPreferences: NotificationPreferences,
    private val notificationRepository: NotificationRepository,
    private val notificationFilterHelper: NotificationFilterHelper
) : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = Resource.Error(throwable)
    }

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

    // Reads filtered and sorted database content and
    // binds it together with a flag calculated from the paging state.
    // Then the uiState is updated triggering UI update.
    private suspend fun filterAndPostNotifications() {
        //println("DEBUG: filterAndPostNotifications called with searchQuery: $currentSearchQuery, " +
        //        "hideRead: ${notificationPreferences.isHideReadNotificationsEnabled()}, " +
        //        "excludedTypes: ${notificationPreferences.getNotificationExcludedTypeCodes()}, "
        //)
        val excludedWikiCodes = notificationPreferences.getNotificationExcludedWikiCodes()
        val includedWikiCodes = notificationFilterHelper.allWikisList().minus(
            excludedWikiCodes
        ).map {
            it.split("-")[0]
        }

        // read from database
        val selectedNotifications = notificationRepository.getAllSelectedNotifications(
            hideReadNotifications = notificationPreferences.isHideReadNotificationsEnabled(),
            searchQuery = currentSearchQuery,
            excludedTypeCodes = notificationPreferences.getNotificationExcludedTypeCodes(),
            includedWikiCodes = includedWikiCodes,
            hideNotMentioned = selectedFilterTab == 1
        )

        //println("filterAndPostNotifications: filterAndPostNotifications - searchQuery: $currentSearchQuery, " +
        //        "hideRead: ${notificationPreferences.isHideReadNotificationsEnabled()}, " +
        //        "excludedTypes: ${notificationPreferences.getNotificationExcludedTypeCodes()}, " +
        //        "includedWikis: $includedWikiCodes selectedFilterTab: $selectedFilterTab yielded " +
        //        "${selectedNotifications.size} notifications from repository."
        //)

        // Containerize the notifications
        val selectionNotificationsListItemContainer = selectedNotifications.map {
            notification -> NotificationListItemContainer(notification)
        }

        // Build a pair
        val pair = Pair(
            selectionNotificationsListItemContainer, !currentContinueStr.isNullOrEmpty()
        )

        _uiState.value = Resource.Success(pair)
    }

    // returns the sum of all excluded wikis plus all excluded type of notifications
    fun excludedFiltersCount(): Int {
        val excludedWikiCodes = notificationPreferences.getNotificationExcludedWikiCodes()
        val excludedTypeCodes = notificationPreferences.getNotificationExcludedTypeCodes()
        return notificationFilterHelper.allWikisList().count { excludedWikiCodes.contains(it) } +
                notificationFilterHelper.allTypesIdList().count { excludedTypeCodes.contains(it) }
    }

    // Resets the paging state and erases in-memory notifications if refresh is requested.
    // Checks connectivity state. If device is online, it fetches data from the API and stores it in
    // the database by calling repository function without an effective filter but with the current
    // paging state.
    // UI update is triggered by calling filterAndPostNotifications()
    fun fetchAndSave(refresh: Boolean = false) {
        if (refresh) {
            currentContinueStr = null
            // notificationList.clear()
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
        /* @todo update
        viewModelScope.launch(handler) {
            notificationList
                .filter { n -> items.map { container -> container.notification?.id }
                    .firstOrNull { it == n.id } != null }
                .map {
                    it.read = if (markUnread) null else Date().toString()
                    notificationRepository.updateNotification(it)
                }
            filterAndPostNotifications()
        }*/
    }
}
