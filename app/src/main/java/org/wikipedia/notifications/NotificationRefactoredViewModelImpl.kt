package org.wikipedia.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertHeaderItem
import androidx.paging.map
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.notifications.db.Notification
import org.wikipedia.util.Resource
import org.wikipedia.util.StringUtil
import java.util.Date

class NotificationRefactoredViewModelImpl(
    private val notificationPreferences: NotificationPreferences,
    private val notificationRepository: NotificationRepository,
    private val notificationFilterHelper: NotificationFilterHelper
) : ViewModel(), NotificationViewModel {
    // Trigger for manual refreshes (e.g., pull-to-refresh)
    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1)
    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = Resource.Error(throwable)
    }

    // map of notification sources: initialized during startup and used for filtering
    private var dbNameMap = mapOf<String, WikiSite>()

    // stores which tab is active: "All" or "Mentions"
    private val _selectedFilterTab = MutableStateFlow(0)
    var selectedFilterTab: Int
        get() = _selectedFilterTab.value
        private set(value) { _selectedFilterTab.value = value }

    // stores whether the search bar is visible in the list
    private val _isSearchVisible = MutableStateFlow(true)
    var isSearchVisible: Boolean
        get() = _isSearchVisible.value
        set(value) { _isSearchVisible.value = value }

    // search query established by user
    private val _currentSearchQuery = MutableStateFlow<String?>(null)
    var currentSearchQuery: String?
        get() = _currentSearchQuery.value
        private set(value) { _currentSearchQuery.value = value }

    // sum of all unread notifications for the "Mentions"-tab
    override var mentionsUnreadCount: Int = 0

    // sum of all unread notification for the "All" tab
    override var allUnreadCount: Int = 0

    // stores whether the end of the list was reached
    var isEndReached: Boolean = false
        private set

    private val _uiState = MutableStateFlow(Resource<Boolean>()
    )
    val uiState = _uiState.asStateFlow()

    /**
     * A reactive stream of paged notification data that orchestrates the entire notification pipeline.
     *
     * This Flow performs the following operations:
     * 1. **Reactive Triggers**: Listens for changes in the search query, active tab (All vs Mentions),
     *    and search bar visibility.
     * 2. **Dynamic Filtering**: When any trigger fires, it re-calculates the included wiki codes
     *    (based on user preferences) and requests a new [PagingData] stream from the repository.
     * 3. **Database-Backed Paging**: The repository provides a [androidx.paging.PagingSource] from Room, which
     *    automatically handles background-thread SQL execution and lazy-loading of pages.
     * 4. **UI Transformation**: Maps raw [Notification] database entities into [NotificationListItemContainer]
     *    UI models. This occurs lazily on a background thread as items enter the viewport.
     * 5. **Header Injection**: Dynamically inserts a search bar header at the top of the list
     *    if [isSearchVisible] is true.
     * 6. **Lifecycle Awareness**: Uses [cachedIn] to survive ViewModel configuration changes (like rotation)
     *    and avoid redundant database queries.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val notificationFlow: Flow<PagingData<NotificationListItemContainer>> = combine(
        _currentSearchQuery,
        _selectedFilterTab,
        _isSearchVisible,
        refreshTrigger.onStart { emit(Unit) }
    ) { query, tab, searchVisible, _ -> Triple(query, tab, searchVisible) }
        .flatMapLatest { (query, tab, searchVisible) ->
            val filters = calcFilters()
            notificationRepository.getNotificationsFlow(
                hideReadNotifications = notificationPreferences.isHideReadNotificationsEnabled(),
                searchQuery = query,
                excludedTypeCodes = filters.excludedTypeCodes,
                includedWikiCodes = filters.includedWikiCodes,
                hideNotMentioned = tab == 1
            ).map { pagingData ->
                val data = pagingData.map { notification ->
                    NotificationListItemContainer(notification)
                }
                // inserts a header item when the search bar shall be displayed
                if (searchVisible) data.insertHeaderItem(item = NotificationListItemContainer()) else data
            }
        }
        .cachedIn(viewModelScope)

    init {
        viewModelScope.launch(handler) {
            dbNameMap = notificationRepository.fetchUnreadWikiDbNames()
            notificationRepository.getRemoteKey(Constants.NOTIFICATIONS_DB_REMOTE_KEY)
        }

        // Observe counts and pagination status reactively
        viewModelScope.launch(handler) {
            // Re-calculate filters inside the collection loop to ensure they reflect preference changes
            combine(
                _selectedFilterTab,
                _currentSearchQuery,
                notificationRepository.getEndOfPaginationReachedFlow()
            ) { _, _, isEndOfPaginationReached -> isEndOfPaginationReached}
                .collectLatest { isEndOfPaginationReached ->
                    isEndReached = isEndOfPaginationReached
                    val filters = calcFilters()
                    notificationRepository.getUnreadCountsFlow(
                        filters.excludedTypeCodes, filters.includedWikiCodes
                    ).collect { (all, mentions) ->
                        allUnreadCount = all
                        mentionsUnreadCount = mentions
                        // Trigger uiState update for the Activity to refresh tab titles
                        _uiState.value = Resource.Success(true)
                    }
                }
        }
    }

    // helper function to calculate current filtering parameters
    private fun calcFilters(): NotificationFilters {
        val excludedTypeCodes = notificationPreferences.getNotificationExcludedTypeCodes()
        val excludedWikiCodes = notificationPreferences.getNotificationExcludedWikiCodes()
        val includedWikiCodes = notificationFilterHelper.allWikisList().minus(excludedWikiCodes).map {
            it.split("-")[0]
        }
        return NotificationFilters(excludedTypeCodes, includedWikiCodes)
    }

    data class NotificationFilters(
        val excludedTypeCodes: Set<String>,
        val includedWikiCodes: List<String>
    )

    // returns the sum of all excluded wikis plus all excluded type of notifications
    fun excludedFiltersCount(): Int {
        val excludedWikiCodes = notificationPreferences.getNotificationExcludedWikiCodes()
        val excludedTypeCodes = notificationPreferences.getNotificationExcludedTypeCodes()
        return notificationFilterHelper.allWikisList().count { excludedWikiCodes.contains(it) } +
                notificationFilterHelper.allTypesIdList().count { excludedTypeCodes.contains(it) }
    }

    // Resets the paging state if refresh is requested.
    // Checks connectivity state. If device is online, it fetches data from the API and stores it in
    // the database by calling repository function without an effective filter but with the current
    // paging state.
    override fun fetchAndSave(refresh: Boolean) {
        if (refresh) {
            viewModelScope.launch {
                refreshTrigger.emit(Unit)
            }
        }
    }

    // Updates currentSearchQuery.
    override fun updateSearchQuery(query: String?) {
        currentSearchQuery = query
    }

    // update the selection of the tab (between "All" and "Mentions"
    override fun updateTabSelection(position: Int) {
        selectedFilterTab = position
    }

    // Marks selected items as read in the local database.
    fun markItemsAsRead(items: List<NotificationListItemContainer>, markUnread: Boolean) {
        val notificationsPerWiki = mutableMapOf<WikiSite, MutableList<Notification>>()
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
        val ids = items.mapNotNull { it.notification?.id }
        viewModelScope.launch(handler) {
            notificationRepository.markItemsAsRead(ids, if (markUnread) null else Date().toString())
            // Paging 3 will automatically refresh due to DB invalidation
        }
    }
}
