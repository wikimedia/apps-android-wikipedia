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
import org.wikipedia.database.AppDatabase
import org.wikipedia.notifications.db.Notification
import org.wikipedia.settings.Prefs
import org.wikipedia.util.StringUtil

class NotificationViewModel : ViewModel() {

    private val notificationRepository = NotificationRepository(AppDatabase.getAppDatabase().notificationDao())
    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = UiState.Error(throwable)
    }
    private val notificationList = mutableListOf<Notification>()
    private var selectedFilterTab: Int = 0
    private var currentContinueStr: String? = null
    private var currentSearchQuery: String? = null
    var mentionsUnreadCount: Int = 0
    var allUnreadCount: Int = 0

    private val _uiState = MutableStateFlow<UiState>(UiState.Success(emptyList(), false))
    val uiState: StateFlow<UiState> = _uiState

    init {
        viewModelScope.launch(handler) {
            collectAllNotifications()
        }
    }

    private suspend fun collectAllNotifications() = notificationRepository.getAllNotifications()
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

        // get unread counts
        allUnreadCount = list.count { it.isUnread }
        mentionsUnreadCount = list.filter { NotificationCategory.isMentionsGroup(it.category) }.count { it.isUnread }

        // Filtered the tab selection
        val filteredList = notificationList.filter { selectedFilterTab == 0 || (selectedFilterTab == 1 && NotificationCategory.isMentionsGroup(it.category)) }

        val notificationContainerList = mutableListOf<NotificationListItemContainer>()

        // Save into display list
        for (n in filteredList) {
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

    fun fetchAndSave(wikiList: String?, filter: String?) {
        viewModelScope.launch(handler) {
            withContext(Dispatchers.IO) {
                currentContinueStr = notificationRepository.fetchAndSave(wikiList, filter, currentContinueStr)
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
        data class Success(val notifications: List<NotificationListItemContainer>, val fromContinuation: Boolean) : UiState()
        data class Error(val throwable: Throwable) : UiState()
    }
}
