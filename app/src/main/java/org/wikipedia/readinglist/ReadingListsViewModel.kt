package org.wikipedia.readinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import org.apache.commons.lang3.StringUtils
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.database.AppDatabase
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.readinglist.database.ReadingListWithPages
import org.wikipedia.readinglist.recommended.RecommendedReadingListUpdateFrequency
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig

enum class SavedTab {
    ALL_ARTICLES,
    COLLECTIONS
}

@OptIn(FlowPreview::class)
class ReadingListsViewModel : ViewModel() {
    private val searchQuery = MutableStateFlow<String?>(null)
    private val debouncedSearchQuery = searchQuery
        .debounce { query -> if (query.isNullOrEmpty()) 0L else SEARCH_DEBOUNCE_MILLIS }
        .distinctUntilChanged()
    private val searchActive = MutableStateFlow(false)
    private val selectedTab = MutableStateFlow(SavedTab.ALL_ARTICLES)

    // Combine the debounced search query and selected tab into a single state flow representing the current content mode.
    // this reduces the number of flows needed in the main combine() call for uiState
    private val contentMode = combine(debouncedSearchQuery, selectedTab) { query, tab ->
        ContentMode(query, tab)
    }.distinctUntilChanged()

    private val recentPreviewSavedState = MutableStateFlow(RecentPreviewSavedState())
    private val accountState = MutableStateFlow(readAccountState())
    private val pageDownloadProgress = MutableStateFlow<Map<Long, Int>>(emptyMap())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectionState = MutableStateFlow(ReadingListsSelectionState())
    val selectionState: StateFlow<ReadingListsSelectionState> = _selectionState.asStateFlow()

    private val readingListsContentState = combine(
        AppDatabase.instance.readingListDao().getListsWithPagesFlow(),
        contentMode,
        recentPreviewSavedState,
        pageDownloadProgress,
        Prefs.observeKeys(R.string.preference_key_reading_list_sort_mode)
    ) { relations, mode, previewSavedState, downloadProgress, _ ->
        ReadingListsUiState(
            isLoading = false,
            rows = buildRows(
                relations,
                mode.tab,
                mode.query,
                previewSavedState.newBadgeListId,
                downloadProgress
            ),
            listCount = relations.size,
            searchQuery = mode.query,
            selectedTab = mode.tab,
            pendingPreviewSavedListId = previewSavedState.pendingSnackbarListId
        )
    }

    private val recommendationPreferenceChanges = Prefs.observeKeys(
        R.string.preference_key_recommended_reading_list_enabled,
        R.string.preference_key_recommended_reading_list_new_list_generated,
        R.string.preference_key_recommended_reading_list_articles_number,
        R.string.preference_key_recommended_reading_list_source,
        R.string.preference_key_recommended_reading_list_update_frequency
    )

    private val discoverCard = combine(
        AppDatabase.instance.recommendedPageDao().getNewRecommendedPagesFlow(),
        recommendationPreferenceChanges,
        accountState
    ) { recommendedPages, _, accountState ->
        if (!Prefs.isRecommendedReadingListEnabled || recommendedPages.isEmpty()) {
            null
        } else {
            RecommendedReadingListCard(
                images = recommendedPages.mapNotNull { it.thumbUrl },
                isNewListGenerated = Prefs.isNewRecommendedReadingListGenerated,
                isUserLoggedIn = accountState.isLoggedIn,
                userName = accountState.userName,
                updateFrequency = Prefs.recommendedReadingListUpdateFrequency
            )
        }
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    val uiState: StateFlow<ReadingListsUiState> =
        combine(
            readingListsContentState,
            searchActive,
            accountState,
            Prefs.observeKeys(
                R.string.preference_key_recommended_reading_list_onboarding_shown,
                R.string.preference_key_sync_reading_lists,
                R.string.preference_key_reading_list_sync_reminder_enabled,
                R.string.preference_key_reading_list_login_reminder_enabled
            ),
            discoverCard
        ) { contentState, isSearchActive, accountState, _, discoverCard ->
            val isSearching = isSearchActive || !contentState.searchQuery.isNullOrEmpty()
            // TODO: confirm with product
            // Right now adding Onboarding and the discover card only to the Collections tab
            val isCollections = contentState.selectedTab == SavedTab.COLLECTIONS
            contentState.copy(
                onboarding = if (isCollections) {
                    resolveOnboardingState(contentState.searchQuery, isSearchActive, accountState)
                } else {
                    OnboardingState.None
                },
                discoverCard = discoverCard.takeIf { isCollections && !isSearching }
            )
        }
            .flowOn(Dispatchers.IO)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = ReadingListsUiState()
            )

    fun setSearchQuery(query: String?) {
        searchQuery.value = query
    }

    fun setSelectedTab(tab: SavedTab) {
        selectedTab.value = tab
    }

    fun setSearchActive(active: Boolean) {
        searchActive.value = active
    }

    fun setRefreshing(refreshing: Boolean) {
        _isRefreshing.value = refreshing
    }

    fun refreshAccountState() {
        accountState.value = readAccountState()
    }

    fun updatePageDownloadProgress(page: ReadingListPage) {
        pageDownloadProgress.value += (page.id to page.downloadProgress)
    }

    fun setSelectionMode(enabled: Boolean) {
        _selectionState.value = ReadingListsSelectionState(enabled = enabled)
    }

    fun toggleListSelection(listId: Long) {
        val selectedListIds = _selectionState.value.selectedListIds.toMutableSet()
        if (!selectedListIds.add(listId)) {
            selectedListIds.remove(listId)
        }
        _selectionState.value = ReadingListsSelectionState(
            enabled = true,
            selectedListIds = selectedListIds
        )
    }

    fun selectAllLists() {
        _selectionState.value = ReadingListsSelectionState(
            enabled = true,
            selectedListIds = uiState.value.rows.filterIsInstance<ReadingListRow.ListRow>()
                .mapTo(mutableSetOf()) { it.list.id }
        )
    }

    fun clearListSelection() {
        _selectionState.value = _selectionState.value.copy(selectedListIds = emptySet())
    }

    fun refreshRecentPreviewSavedList() {
        val receivedListId = Prefs.readingListRecentReceivedId
        if (receivedListId == -1L || recentPreviewSavedState.value.newBadgeListId != null) {
            return
        }
        recentPreviewSavedState.value = RecentPreviewSavedState(
            pendingSnackbarListId = receivedListId,
            newBadgeListId = receivedListId
        )
    }

    fun consumePreviewSavedSnackbar(listId: Long) {
        val currentState = recentPreviewSavedState.value
        if (currentState.pendingSnackbarListId != listId) {
            return
        }
        Prefs.receiveReadingListsData = null
        Prefs.readingListRecentReceivedId = -1L
        recentPreviewSavedState.value = currentState.copy(pendingSnackbarListId = null)
    }

    fun clearRecentPreviewSavedList() {
        Prefs.readingListRecentReceivedId = -1L
        recentPreviewSavedState.value = RecentPreviewSavedState()
    }

    fun containingLists(pageId: Long): List<ContainingList> {
        return uiState.value.rows
            .filterIsInstance<ReadingListRow.PageRow>()
            .firstOrNull { it.page.id == pageId }
            ?.containingLists
            .orEmpty()
    }

    fun setSortMode(sortMode: Int) {
        Prefs.setReadingListSortMode(
            when (sortMode) {
                ReadingList.SORT_BY_NAME_DESC,
                ReadingList.SORT_BY_RECENT_DESC,
                ReadingList.SORT_BY_RECENT_ASC,
                ReadingList.SORT_BY_NAME_ASC -> sortMode
                else -> ReadingList.SORT_BY_NAME_ASC
            }
        )
    }

    private fun resolveOnboardingState(
        query: String?,
        isSearchActive: Boolean,
        accountState: ReadingListsAccountState
    ): OnboardingState {
        if (isSearchActive || !query.isNullOrEmpty()) {
            return OnboardingState.None
        }
        return when {
            !Prefs.isRecommendedReadingListOnboardingShown -> {
                OnboardingState.RecommendedReadingList
            }
            (accountState.isLoggedIn && !accountState.isTemporaryAccount) && !Prefs.isReadingListSyncEnabled &&
                    Prefs.isReadingListSyncReminderEnabled && !RemoteConfig.config.disableReadingListSync -> {
                OnboardingState.SyncReminder
            }
            (!accountState.isLoggedIn || accountState.isTemporaryAccount) && Prefs.isReadingListLoginReminderEnabled &&
                    !RemoteConfig.config.disableReadingListSync -> {
                OnboardingState.LoginReminder
            }
            else -> OnboardingState.None
        }
    }

    private fun readAccountState(): ReadingListsAccountState {
        return ReadingListsAccountState(
            isLoggedIn = AccountUtil.isLoggedIn,
            isTemporaryAccount = AccountUtil.isTemporaryAccount,
            userName = AccountUtil.userName
        )
    }

    private fun buildRows(
        relations: List<ReadingListWithPages>,
        tab: SavedTab,
        query: String?,
        recentPreviewSavedId: Long?,
        downloadProgress: Map<Long, Int>
    ): List<ReadingListRow> {
        // toReadingList() drops pages queued for deletion (Room @Relation can't filter children in SQL)
        val lists = relations.map { it.toReadingList() }.toMutableList()

        if (!query.isNullOrEmpty()) {
            return buildSearchRows(lists, query, recentPreviewSavedId, downloadProgress)
        }

        return when (tab) {
            SavedTab.COLLECTIONS -> buildCollectionsRows(lists, recentPreviewSavedId)
            SavedTab.ALL_ARTICLES -> buildArticleRows(lists, downloadProgress)
        }
    }

    private fun buildCollectionsRows(
        lists: MutableList<ReadingList>,
        recentPreviewSavedId: Long?
    ): List<ReadingListRow> {
        ReadingList.sort(lists, Prefs.getReadingListSortMode(ReadingList.SORT_BY_NAME_ASC))
        lists.removeEmptyDefaultList()
        return lists.map { ReadingListRow.ListRow(it.toUiModel(recentPreviewSavedId)) }
    }

    // filters the list rows first with stripAccents from the list and ReadingList title
    // then creates list of ListRow and PageRow for the search results
    private fun buildSearchRows(
        lists: List<ReadingList>,
        query: String,
        recentPreviewSavedId: Long?,
        downloadProgress: Map<Long, Int>
    ): List<ReadingListRow> {
        val normalizedQuery = StringUtils.stripAccents(query)
        val listRows = lists
            .filter { it.accentInvariantTitle.contains(normalizedQuery, ignoreCase = true) }
            .map { ReadingListRow.ListRow(it.toUiModel(recentPreviewSavedId)) }
        val pageRows = buildArticleRows(lists, downloadProgress, normalizedQuery)
        return listRows + pageRows
    }

    /**
     * Every saved article across [lists] as [ReadingListRow.PageRow]s, de-duplicated by lang + API
     * title so an article on multiple lists appears once, annotated with the lists that contain it.
     * When [titleFilter] is non-null, only articles whose title contains it (accent-insensitive) are kept.
     */
    private fun buildArticleRows(
        lists: List<ReadingList>,
        downloadProgress: Map<Long, Int>,
        titleFilter: String? = null
    ): List<ReadingListRow.PageRow> {
        // Map each article (lang + API title) to the lists that contain it, so we resolve the
        // containing lists once instead of re-scanning every list per article.
        val containingListsByPage = mutableMapOf<Pair<String, String>, MutableList<ContainingList>>()
        lists.forEach { list ->
            val containingList = ContainingList(list.id, list.title)
            list.pages.forEach { page ->
                containingListsByPage.getOrPut(page.lang to page.apiTitle) { mutableListOf() }
                    .add(containingList)
            }
        }

        val seenPages = mutableSetOf<Pair<String, String>>()
        val pageRows = mutableListOf<ReadingListRow.PageRow>()
        lists.forEach { list ->
            list.pages.forEach { page ->
                val matches = titleFilter == null ||
                    page.accentInvariantTitle.contains(titleFilter, ignoreCase = true)
                if (matches && seenPages.add(page.lang to page.apiTitle)) {
                    pageRows.add(
                        ReadingListRow.PageRow(
                            page.toUiModel(downloadProgress[page.id] ?: 0),
                            containingListsByPage[page.lang to page.apiTitle].orEmpty()
                        )
                    )
                }
            }
        }
        return pageRows
    }

    private fun MutableList<ReadingList>.removeEmptyDefaultList() {
        if (size == 1 && this[0].isDefault && this[0].pages.isEmpty()) {
            removeAt(0)
        }
    }

    private fun ReadingList.toUiModel(recentPreviewSavedId: Long?): ReadingListUiModel {
        return ReadingListUiModel(
            id = id,
            title = title,
            description = description,
            isDefault = isDefault,
            totalPages = pages.size,
            sizeBytesFromPages = sizeBytesFromPages,
            thumbUrls = pages.mapNotNull { it.thumbUrl }.filterNot { it.isEmpty() },
            isNew = id == recentPreviewSavedId
        )
    }

    private fun ReadingListPage.toUiModel(downloadProgress: Int): ReadingListPageUiModel {
        val saving = saving
        return ReadingListPageUiModel(
            id = id,
            title = displayTitle,
            description = description,
            thumbUrl = thumbUrl,
            lang = lang,
            apiTitle = apiTitle,
            offline = offline,
            saving = saving,
            downloadProgress = downloadProgress,
            isAvailable = WikipediaApp.instance.isOnline || (offline && !saving)
        )
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5000L
        private const val SEARCH_DEBOUNCE_MILLIS = 150L
    }
}

/**
 * Immutable snapshot of a reading list for rendering, used for [ReadingListRow.ListRow].
 */
data class ReadingListUiModel(
    val id: Long,
    val title: String,
    val description: String?,
    val isDefault: Boolean,
    val totalPages: Int,
    val sizeBytesFromPages: Long,
    val thumbUrls: List<String> = emptyList(),
    val isNew: Boolean = false
)

/** Immutable snapshot of a saved article, used for [ReadingListRow.PageRow] while searching. */
data class ReadingListPageUiModel(
    val id: Long,
    val title: String,
    val description: String?,
    val thumbUrl: String?,
    val lang: String,
    val apiTitle: String,
    val offline: Boolean,
    val saving: Boolean,
    val downloadProgress: Int,
    val isAvailable: Boolean
)

/**
 * A single row in the "Saved" list. Normally the screen only shows [ListRow]s; while searching it
 * shows matching [ListRow]s followed by matching [PageRow]s (articles found across all lists).
 */
sealed interface ReadingListRow {
    data class ListRow(val list: ReadingListUiModel) : ReadingListRow
    data class PageRow(val page: ReadingListPageUiModel, val containingLists: List<ContainingList>) : ReadingListRow
}
data class ContainingList(val id: Long, val title: String)

data class ReadingListsUiState(
    val isLoading: Boolean = true,
    val rows: List<ReadingListRow> = emptyList(),
    val listCount: Int = 0,
    val searchQuery: String? = null,
    val selectedTab: SavedTab = SavedTab.COLLECTIONS,
    val onboarding: OnboardingState = OnboardingState.None,
    val discoverCard: RecommendedReadingListCard? = null,
    val pendingPreviewSavedListId: Long? = null
)

private data class ContentMode(
    val query: String?,
    val tab: SavedTab
)

data class RecentPreviewSavedState(
    val pendingSnackbarListId: Long? = null,
    val newBadgeListId: Long? = null
)

data class RecommendedReadingListCard(
    val images: List<String>,
    val isNewListGenerated: Boolean,
    val isUserLoggedIn: Boolean,
    val userName: String,
    val updateFrequency: RecommendedReadingListUpdateFrequency
)

data class ReadingListsSelectionState(
    val enabled: Boolean = false,
    val selectedListIds: Set<Long> = emptySet()
)

private data class ReadingListsAccountState(
    val isLoggedIn: Boolean,
    val isTemporaryAccount: Boolean,
    val userName: String
)

sealed interface OnboardingState {
    data object None : OnboardingState
    data object RecommendedReadingList : OnboardingState
    data object SyncReminder : OnboardingState
    data object LoginReminder : OnboardingState
}
