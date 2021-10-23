package org.wikipedia.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.notifications.db.Notification
import org.wikipedia.settings.Prefs
import org.wikipedia.util.StringUtil

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

    private val _uiState = MutableStateFlow<UiState>(UiState.Success(emptyList(), emptyMap(), false))
    val uiState: StateFlow<UiState> = _uiState

    init {
        viewModelScope.launch(handler) {
            withContext(Dispatchers.IO) {
                dbNameMap = notificationRepository.fetchUnreadWikiDbNames()
            }
            collectAllNotifications()
        }
    }

    private suspend fun collectAllNotifications() = notificationRepository.getAllNotifications()
        .collect { list ->
            _uiState.value = UiState.Success(processList(list), dbNameMap, !currentContinueStr.isNullOrEmpty())
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

        // get unread counts
        allUnreadCount = list.count { it.isUnread }
        mentionsUnreadCount = list.filter { NotificationCategory.isMentionsGroup(it.category) }.count { it.isUnread }

        // Filtered the tab selection
        val filteredList = notificationList.filter { selectedFilterTab == 0 || (selectedFilterTab == 1 && NotificationCategory.isMentionsGroup(it.category)) }

        val notificationContainerList = mutableListOf<NotificationListItemContainer>()

        // Save into display list
        for (n in filteredList.filter { delimitedFilteredWikiList().contains(it.wiki) }) {
            val linkText = n.contents?.links?.secondary?.firstOrNull()?.label
            val searchQuery = currentSearchQuery
            if (!searchQuery.isNullOrEmpty() &&
                !(n.title?.full?.contains(searchQuery, true) == true ||
                        n.contents?.header?.contains(searchQuery, true) == true ||
                        n.contents?.body?.contains(searchQuery, true) == true ||
                        (linkText?.contains(searchQuery, true) == true))) {
                continue
            }
            val filterList = mutableListOf<String>()
            filterList.addAll(StringUtil.csvToList(Prefs.notificationsFilterLanguageCodes.orEmpty()).filter { NotificationCategory.isFiltersGroup(it) })
            if (filterList.contains(n.category) || Prefs.notificationsFilterLanguageCodes == null) notificationContainerList.add(NotificationListItemContainer(n))
        }
        return notificationContainerList
    }

    // TODO: save as a variable and not run this every time when fetching API.
    private fun delimitedFilteredWikiList(): List<String> {
        val filteredWikiList = mutableListOf<String>()
        if (Prefs.notificationsFilterLanguageCodes == null) {
            WikipediaApp.getInstance().language().appLanguageCodes.forEach {
                val defaultLangCode = WikipediaApp.getInstance().language().getDefaultLanguageCode(it) ?: it
                filteredWikiList.add("${defaultLangCode.replace("-", "_")}wiki")
            }
            filteredWikiList.add("commonswiki")
            filteredWikiList.add("wikidatawiki")
        } else {
            val wikiTypeList = StringUtil.csvToList(Prefs.notificationsFilterLanguageCodes.orEmpty())
            wikiTypeList.filter { WikipediaApp.getInstance().language().appLanguageCodes.contains(it) }.forEach { langCode ->
                val defaultLangCode = WikipediaApp.getInstance().language().getDefaultLanguageCode(langCode) ?: langCode
                filteredWikiList.add("${defaultLangCode.replace("-", "_")}wiki")
            }
            wikiTypeList.filter { it == "commons" || it == "wikidata" }.forEach { langCode ->
                filteredWikiList.add("${langCode}wiki")
            }
        }
        return filteredWikiList
    }

    fun fetchAndSave() {
        viewModelScope.launch(handler) {
            // TODO: skip the loading?
            withContext(Dispatchers.IO) {
                // TODO: fetch all notifications from all wiki sites
                currentContinueStr = notificationRepository.fetchAndSave(delimitedFilteredWikiList().joinToString("|"), "read|!read", currentContinueStr)
            }
            // TODO: revisit this
            collectAllNotifications()
        }
    }

    fun updateTabSelection(position: Int) {
        selectedFilterTab = position
        viewModelScope.launch(handler) {
            collectAllNotifications()
        }
    }

    sealed class UiState {
        data class Success(val notifications: List<NotificationListItemContainer>,
                           val dbNameMap: Map<String, WikiSite>,
                           val fromContinuation: Boolean) : UiState()
        data class Error(val throwable: Throwable) : UiState()
    }
}
