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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
import kotlin.time.Duration.Companion.milliseconds

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
        Prefs.observeKeys(
            R.string.preference_key_reading_list_sort_mode,
            R.string.preference_key_reading_list_page_sort_mode
        )
    ) { relations, mode, previewSavedState, _ ->
        val sortMode = when (mode.tab) {
            SavedTab.ALL_ARTICLES -> Prefs.getReadingListPageSortMode(ReadingList.SORT_BY_NAME_ASC)
            SavedTab.COLLECTIONS -> Prefs.getReadingListSortMode(ReadingList.SORT_BY_NAME_ASC)
        }
        ReadingListsUiState(
            isLoading = false,
            rows = buildRows(
                relations,
                mode.tab,
                mode.query,
                previewSavedState.newBadgeListId,
                sortMode
            ),
            listCount = relations.size,
            searchQuery = mode.query,
            selectedTab = mode.tab,
            sortMode = sortMode,
            pendingPreviewSavedListId = previewSavedState.pendingSnackbarListId
        )
    }

    // SavedPageSyncService emits PageDownloadEvent which updates downloadProgress in ReadingListPage during a sync
    // this only exposes the presentation state of the download progress for visible article rows,
    // which avoids expensive row-building combine above on each update
    private val contentStateWithDownloadProgress = combine(
        readingListsContentState,
        pageDownloadProgress
            .sample(PROGRESS_SAMPLE_MILLIS.milliseconds)
            .onStart { emit(pageDownloadProgress.value) }
            .distinctUntilChanged()
    ) { contentState, downloadProgress ->
        contentState.copy(
            pageDownloadProgress = if (contentState.selectedTab == SavedTab.ALL_ARTICLES) {
                downloadProgress
            } else {
                emptyMap()
            }
        )
    }.distinctUntilChanged()

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
            contentStateWithDownloadProgress,
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
                isSearchActive = isSearchActive,
                onboarding = resolveOnboardingState(contentState.selectedTab, contentState.searchQuery, isSearchActive, accountState),
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
        pageDownloadProgress.update { progressByPageId ->
            if (progressByPageId[page.id] == page.downloadProgress) {
                progressByPageId
            } else {
                progressByPageId + (page.id to page.downloadProgress)
            }
        }
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

    fun togglePageSelection(pageId: Long) {
        val selectedPageIds = _selectionState.value.selectedPageIds.toMutableSet()
        if (!selectedPageIds.add(pageId)) {
            selectedPageIds.remove(pageId)
        }
        _selectionState.value = ReadingListsSelectionState(
            enabled = true,
            selectedPageIds = selectedPageIds
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

    fun selectAllPages() {
        _selectionState.value = ReadingListsSelectionState(
            enabled = true,
            selectedPageIds = uiState.value.rows.filterIsInstance<ReadingListRow.PageRow>()
                .mapTo(mutableSetOf()) { it.page.id }
        )
    }

    fun clearPageSelection() {
        _selectionState.value = _selectionState.value.copy(selectedPageIds = emptySet())
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
        when (selectedTab.value) {
            SavedTab.ALL_ARTICLES -> Prefs.setReadingListPageSortMode(sortMode)
            SavedTab.COLLECTIONS -> Prefs.setReadingListSortMode(sortMode)
        }
    }

    private fun resolveOnboardingState(
        selectedTab: SavedTab,
        query: String?,
        isSearchActive: Boolean,
        accountState: ReadingListsAccountState
    ): OnboardingState {
        if (isSearchActive || !query.isNullOrEmpty()) {
            return OnboardingState.None
        }

        return when (selectedTab) {
            SavedTab.ALL_ARTICLES -> when {
                accountState.isLoggedIn && !accountState.isTemporaryAccount && !Prefs.isReadingListSyncEnabled &&
                        Prefs.isReadingListSyncReminderEnabled && !RemoteConfig.config.disableReadingListSync -> {
                    OnboardingState.SyncReminder
                }
                (!accountState.isLoggedIn || accountState.isTemporaryAccount) && Prefs.isReadingListLoginReminderEnabled &&
                        !RemoteConfig.config.disableReadingListSync -> {
                    OnboardingState.LoginReminder
                }
                else -> OnboardingState.None
            }
            SavedTab.COLLECTIONS -> {
                if (!Prefs.isRecommendedReadingListOnboardingShown) {
                    OnboardingState.RecommendedReadingList
                } else {
                    OnboardingState.None
                }
            }
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
        sortMode: Int
    ): List<ReadingListRow> {
        // toReadingList() drops pages queued for deletion (Room @Relation can't filter children in SQL)
        val lists = relations.map { it.toReadingList() }.toMutableList()

        if (!query.isNullOrEmpty()) {
            return buildSearchRows(lists, tab, query, recentPreviewSavedId)
        }

        return when (tab) {
            SavedTab.COLLECTIONS -> buildCollectionsRows(lists, recentPreviewSavedId, sortMode)
            SavedTab.ALL_ARTICLES -> buildArticleRows(lists, sortMode = sortMode)
        }
    }

    private fun buildCollectionsRows(
        lists: MutableList<ReadingList>,
        recentPreviewSavedId: Long?,
        sortMode: Int
    ): List<ReadingListRow> {
        ReadingList.sort(lists, sortMode)
        lists.removeEmptyDefaultList()
        return lists.map { ReadingListRow.ListRow(it.toUiModel(recentPreviewSavedId)) }
    }

    private fun buildSearchRows(
        lists: List<ReadingList>,
        tab: SavedTab,
        query: String,
        recentPreviewSavedId: Long?
    ): List<ReadingListRow> {
        val normalizedQuery = StringUtils.stripAccents(query)
        return when (tab) {
            SavedTab.COLLECTIONS -> lists
                .filter { it.accentInvariantTitle.contains(normalizedQuery, ignoreCase = true) }
                .map { ReadingListRow.ListRow(it.toUiModel(recentPreviewSavedId)) }
            SavedTab.ALL_ARTICLES -> buildArticleRows(lists, normalizedQuery)
        }
    }

    /**
     * Every saved article across [lists] as [ReadingListRow.PageRow]s, de-duplicated by lang + API
     * title so an article on multiple lists appears once, annotated with the lists that contain it.
     * When [titleFilter] is non-null, only articles whose title contains it (accent-insensitive) are kept.
     */
    private fun buildArticleRows(
        lists: List<ReadingList>,
        titleFilter: String? = null,
        sortMode: Int? = null
    ): List<ReadingListRow.PageRow> {
        val selectedArticles = linkedMapOf<Pair<String, String>, ArticleRowData>()
        lists.forEach { list ->
            list.pages.forEach { page ->
                val matches = titleFilter == null ||
                    page.accentInvariantTitle.contains(titleFilter, ignoreCase = true)
                if (matches) {
                    selectedArticles.getOrPut(page.lang to page.apiTitle) { ArticleRowData(page) }
                }
            }
        }

        if (selectedArticles.isEmpty()) {
            return emptyList()
        }

        lists.forEach { list ->
            val containingList = ContainingList(list.id, list.title)
            list.pages.forEach { page ->
                selectedArticles[page.lang to page.apiTitle]?.containingLists?.add(containingList)
            }
        }

        val orderedPages = selectedArticles.values.mapTo(mutableListOf()) { it.page }
        sortMode?.let { ReadingList.sortPages(orderedPages, it) }

        return orderedPages.map { page ->
            val article = selectedArticles.getValue(page.lang to page.apiTitle)
            ReadingListRow.PageRow(
                page.toUiModel(),
                article.containingLists
            )
        }
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

    private fun ReadingListPage.toUiModel(): ReadingListPageUiModel {
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
            isAvailable = WikipediaApp.instance.isOnline || (offline && !saving)
        )
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5000L
        private const val SEARCH_DEBOUNCE_MILLIS = 150L
        private const val PROGRESS_SAMPLE_MILLIS = 150L
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
    val isSearchActive: Boolean = false,
    val rows: List<ReadingListRow> = emptyList(),
    val pageDownloadProgress: Map<Long, Int> = emptyMap(),
    val listCount: Int = 0,
    val searchQuery: String? = null,
    val selectedTab: SavedTab = SavedTab.ALL_ARTICLES,
    val sortMode: Int = ReadingList.SORT_BY_NAME_ASC,
    val onboarding: OnboardingState = OnboardingState.None,
    val discoverCard: RecommendedReadingListCard? = null,
    val pendingPreviewSavedListId: Long? = null
)

private data class ContentMode(
    val query: String?,
    val tab: SavedTab
)

private data class ArticleRowData(
    val page: ReadingListPage,
    val containingLists: MutableList<ContainingList> = mutableListOf()
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
    val selectedListIds: Set<Long> = emptySet(),
    val selectedPageIds: Set<Long> = emptySet()
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
