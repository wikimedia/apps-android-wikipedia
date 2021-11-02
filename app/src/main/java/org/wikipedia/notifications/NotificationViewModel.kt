package org.wikipedia.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.NotificationInteractionFunnel
import org.wikipedia.analytics.eventplatform.NotificationInteractionEvent
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.notifications.db.Notification
import org.wikipedia.settings.Prefs
import org.wikipedia.util.StringUtil
import java.util.*

class NotificationViewModel : ViewModel() {

    private val notificationRepository = NotificationRepository(AppDatabase.getAppDatabase().notificationDao())
    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = UiState.Error(throwable)
    }
    private val notificationList = mutableListOf<Notification>()
    private var dbNameMap = mapOf<String, WikiSite>()
    private var selectedFilterTab: Int = 0
    private var currentContinueStr: String? = null
    private var currentSearchQuery: String? = null
    var mentionsUnreadCount: Int = 0
    var allUnreadCount: Int = 0

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState

    init {
        viewModelScope.launch(handler) {
            withContext(Dispatchers.IO) {
                dbNameMap = notificationRepository.fetchUnreadWikiDbNames()
            }
        }
    }

    private suspend fun collectionNotifications() = notificationRepository.getAllNotifications()
        .collect { list ->
            _uiState.value = UiState.Success(processList(list), !currentContinueStr.isNullOrEmpty())
        }

    private fun processList(list: List<Notification>): List<NotificationListItemContainer> {

        // Reduce duplicate notifications
        if (currentContinueStr.isNullOrEmpty()) {
            notificationList.clear()
        }
        for (n in list) {
            if (notificationList.none { it.id == n.id }) {
                notificationList.add(n)
            }
        }
        // Sort them by descending date...
        notificationList.sortWith { n1, n2 -> n2.date().compareTo(n1.date()) }

        updateTabUnreadCounts()

        // Filtered the tab selection
        val tabSelectedList = notificationList
            .filter { if (Prefs.hideReadNotificationsEnabled) it.isUnread else true }
            .filter { selectedFilterTab == 0 || (selectedFilterTab == 1 && NotificationCategory.isMentionsGroup(it.category)) }

        val excludedTypeCodes = Prefs.notificationExcludedTypeCodes
        val excludedWikiCodes = Prefs.notificationExcludedWikiCodes
        val includedWikiCodes = NotificationsFilterActivity.allWikisList().minus(excludedWikiCodes).map {
            it.split("-")[0]
        }
        val checkExcludedWikiCodes = NotificationsFilterActivity.allWikisList().size != includedWikiCodes.size

        val notificationContainerList = mutableListOf<NotificationListItemContainer>()

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
            notificationContainerList.add(NotificationListItemContainer(n))
        }
        return notificationContainerList
    }

    private fun updateTabUnreadCounts() {
        allUnreadCount = notificationList.count { it.isUnread }
        mentionsUnreadCount = notificationList.filter { NotificationCategory.isMentionsGroup(it.category) }.count { it.isUnread }
    }

    private fun delimitedWikiList(): String {
        return dbNameMap.keys.union(NotificationsFilterActivity.allWikisList().map {
            val defaultLangCode = WikipediaApp.getInstance().language().getDefaultLanguageCode(it) ?: it
            "${defaultLangCode.replace("-", "_")}wiki"
        }).joinToString("|")
    }

    fun excludedFiltersCount(): Int {
        val excludedWikiCodes = Prefs.notificationExcludedWikiCodes
        val excludedTypeCodes = Prefs.notificationExcludedTypeCodes
        return NotificationsFilterActivity.allWikisList().count { excludedWikiCodes.contains(it) } +
                NotificationsFilterActivity.allTypesIdList().count { excludedTypeCodes.contains(it) }
    }

    fun fetchAndSave() {
        viewModelScope.launch(handler) {
            if (WikipediaApp.getInstance().isOnline) {
                withContext(Dispatchers.IO) {
                    // TODO: fetch "all" wikis - update after the changes merging to the main branch.
                    currentContinueStr = notificationRepository.fetchAndSave(delimitedWikiList(), "read|!read", currentContinueStr)
                }
            }
            collectionNotifications()
        }
    }

    fun updateTabSelection(position: Int) {
        selectedFilterTab = position
        viewModelScope.launch(handler) {
            collectionNotifications()
        }
    }

    fun markItemsAsRead(items: List<NotificationListItemContainer>, markUnread: Boolean) {
        val notificationsPerWiki = mutableMapOf<WikiSite, MutableList<Notification>>()
        val selectionKey = if (items.size > 1) Random().nextLong() else null
        for (item in items) {
            val notification = item.notification!!
            val wiki = dbNameMap.getOrElse(notification.wiki) {
                when (notification.wiki) {
                    "commonswiki" -> WikiSite(Service.COMMONS_URL)
                    "wikidatawiki" -> WikiSite(Service.WIKIDATA_URL)
                    else -> {
                        val langCode = StringUtil.dbNameToLangCode(notification.wiki)
                        WikiSite.forLanguageCode(WikipediaApp.getInstance().language().getDefaultLanguageCode(langCode) ?: langCode)
                    }
                }
            }
            notificationsPerWiki.getOrPut(wiki) { ArrayList() }.add(notification)
            if (!markUnread) {
                NotificationInteractionFunnel(WikipediaApp.getInstance(), notification).logMarkRead(selectionKey)
                NotificationInteractionEvent.logMarkRead(notification, selectionKey)
            }
        }

        for (wiki in notificationsPerWiki.keys) {
            NotificationPollBroadcastReceiver.markRead(wiki, notificationsPerWiki[wiki]!!, markUnread)
        }

        // Mark items in read state and save into database
        notificationList
            .filter { n -> items.map { container -> container.notification?.id }
            .firstOrNull { it == n.id } != null }
            .map {
                it.read = if (markUnread) null else Date().toString()
                viewModelScope.launch(handler) {
                    withContext(Dispatchers.IO) {
                        notificationRepository.updateNotification(it)
                    }
                    collectionNotifications()
                    updateTabUnreadCounts()
                }
            }
    }

    open class UiState {
        data class Success(val notifications: List<NotificationListItemContainer>,
                           val fromContinuation: Boolean) : UiState()
        data class Error(val throwable: Throwable) : UiState()
    }
}
