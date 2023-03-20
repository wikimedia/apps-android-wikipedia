package org.wikipedia.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.NotificationInteractionEvent
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.notifications.db.Notification
import org.wikipedia.settings.Prefs
import org.wikipedia.util.StringUtil
import java.util.*

class NotificationViewModel : ViewModel() {

    private val notificationRepository = NotificationRepository(AppDatabase.instance.notificationDao())
    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = UiState.Error(throwable)
    }
    private val notificationList = mutableListOf<Notification>()
    private var dbNameMap = mapOf<String, WikiSite>()
    private var selectedFilterTab: Int = 0
    private var currentContinueStr: String? = null
    var currentSearchQuery: String? = null
        private set
    var mentionsUnreadCount: Int = 0
    var allUnreadCount: Int = 0

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch(handler) {
            dbNameMap = notificationRepository.fetchUnreadWikiDbNames()
        }
        fetchAndSave()
    }

    private fun filterAndPostNotifications() {
        _uiState.value = UiState.Success(processList(notificationRepository.getAllNotifications()),
            !currentContinueStr.isNullOrEmpty())
    }

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
        notificationList.sortByDescending { it.instant() }

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

    private fun delimitedWikiList(): String {
        return dbNameMap.keys.union(NotificationFilterActivity.allWikisList().map {
            val defaultLangCode = WikipediaApp.instance.languageState.getDefaultLanguageCode(it) ?: it
            "${defaultLangCode.replace("-", "_")}wiki"
        }).joinToString("|")
    }

    fun excludedFiltersCount(): Int {
        val excludedWikiCodes = Prefs.notificationExcludedWikiCodes
        val excludedTypeCodes = Prefs.notificationExcludedTypeCodes
        return NotificationFilterActivity.allWikisList().count { excludedWikiCodes.contains(it) } +
                NotificationFilterActivity.allTypesIdList().count { excludedTypeCodes.contains(it) }
    }

    fun fetchAndSave(refresh: Boolean = false) {
        if (refresh) {
            currentContinueStr = null
            notificationList.clear()
        }

        viewModelScope.launch(handler) {
            if (WikipediaApp.instance.isOnline) {
                currentContinueStr = notificationRepository.fetchAndSave(delimitedWikiList(), "read|!read", currentContinueStr)
            }
            filterAndPostNotifications()
        }
    }

    fun updateSearchQuery(query: String?) {
        currentSearchQuery = query
        viewModelScope.launch(handler) {
            filterAndPostNotifications()
        }
    }

    fun updateTabSelection(position: Int) {
        selectedFilterTab = position
        viewModelScope.launch(handler) {
            filterAndPostNotifications()
        }
    }

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
            if (!markUnread) {
                NotificationInteractionEvent.logMarkRead(notification, selectionKey)
            }
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

    open class UiState {
        class Success(val notifications: List<NotificationListItemContainer>,
                      val fromContinuation: Boolean) : UiState()
        class Error(val throwable: Throwable) : UiState()
    }
}
