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
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.readinglist.recommended.RecommendedReadingListUpdateFrequency
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig
import kotlin.time.Duration.Companion.milliseconds

enum class SavedTab {
    ALL_ARTICLES,
    COLLECTIONS
}

enum class SavedArticleFilter {
    ALL_ARTICLES,
    NOT_IN_COLLECTION
}

@OptIn(FlowPreview::class)
class ReadingListsViewModel : ViewModel() {
    private val searchQuery = MutableStateFlow<String?>(null)
    private val debouncedSearchQuery = searchQuery
        .debounce { query -> if (query.isNullOrEmpty()) 0L else SEARCH_DEBOUNCE_MILLIS }
        .distinctUntilChanged()
    private val searchActive = MutableStateFlow(false)
    private val selectedTab = MutableStateFlow(SavedTab.ALL_ARTICLES)
    private val selectedArticleFilter = MutableStateFlow(SavedArticleFilter.ALL_ARTICLES)

    // Combine the debounced search query, selected tab, and article filter into a single state flow representing the current content mode.
    // this reduces the number of flows needed in the main combine() call for uiState
    private val contentMode = combine(debouncedSearchQuery, selectedTab, selectedArticleFilter) { query, tab, filter ->
        ContentMode(query, tab, filter)
    }.distinctUntilChanged()

    private val recentPreviewSavedState = MutableStateFlow(RecentPreviewSavedState())

    private val accountState = MutableStateFlow(readAccountState())
    private val pageDownloadProgress = MutableStateFlow<Map<ArticleKey, Int>>(emptyMap())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectionState = MutableStateFlow(ReadingListsSelectionState())
    val selectionState: StateFlow<ReadingListsSelectionState> = _selectionState.asStateFlow()
    private val selectedArticleKeys = mutableSetOf<ArticleKey>()

    private val readingListsContentState = combine(
        AppDatabase.instance.readingListDao().getListsWithPagesFlow(),
        ArticleOfflineStateStore(AppDatabase.instance).observe(),
        contentMode,
        recentPreviewSavedState,
        Prefs.observeKeys(
            R.string.preference_key_reading_list_sort_mode,
            R.string.preference_key_reading_list_page_sort_mode
        )
    ) { relations, offlineStates, mode, previewSavedState, _ ->
        val sortMode = when (mode.tab) {
            SavedTab.ALL_ARTICLES -> Prefs.getReadingListPageSortMode(ReadingList.SORT_BY_NAME_ASC)
            SavedTab.COLLECTIONS -> Prefs.getReadingListSortMode(ReadingList.SORT_BY_NAME_ASC)
        }
        // toReadingList() drops pages queued for deletion (Room @Relation can't filter children in SQL)
        val lists = relations.map { relation ->
            relation.toReadingList().apply { pages.withOfflineState(offlineStates) }
        }.toMutableList()
        val hasSavedArticles = lists.any { it.pages.isNotEmpty() }
        val rows = buildRows(lists, mode.tab, mode.query, mode.articleFilter, previewSavedState.newBadgeListId, sortMode)
        val articleIds = lists.flatMap { it.pages }.groupBy { it.articleKey() }
            .mapValues { (_, pages) -> pages.minOf { it.id } }.toMutableMap()
        rows.filterIsInstance<ReadingListRow.PageRow>().forEach { articleIds[it.articleKey] = it.page.id }
        ReadingListsUiState(
            isLoading = false,
            rows = rows,
            hasSavedArticles = hasSavedArticles,
            articleIds = articleIds,
            listCount = relations.size,
            searchQuery = mode.query,
            selectedTab = mode.tab,
            selectedArticleFilter = mode.articleFilter,
            sortMode = sortMode,
            showCollectionsBadge = previewSavedState.newBadgeListId != null,
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
                contentState.rows.filterIsInstance<ReadingListRow.PageRow>().associate { it.page.id to (downloadProgress[it.articleKey] ?: 0) }
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
            val isCollections = contentState.selectedTab == SavedTab.COLLECTIONS
            contentState.copy(
                isSearchActive = isSearchActive,
                onboarding = resolveOnboardingState(contentState.selectedTab, contentState.searchQuery, isSearchActive, accountState),
                discoverCard = discoverCard.takeIf { isCollections && !isSearching },
                showCollectionsBadge = contentState.showCollectionsBadge || discoverCard?.isNewListGenerated == true
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

    fun isSelectedTab(tab: SavedTab): Boolean {
        return selectedTab.value == tab
    }

    fun setArticleFilter(filter: SavedArticleFilter) {
        selectedArticleFilter.value = filter
    }

    fun getSelectedArticleFilter(): SavedArticleFilter {
        return selectedArticleFilter.value
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
            if (progressByPageId[page.articleKey()] == page.downloadProgress) {
                progressByPageId
            } else {
                progressByPageId + (page.articleKey() to page.downloadProgress)
            }
        }
    }

    fun setSelectionMode(enabled: Boolean) {
        selectedArticleKeys.clear()
        _selectionState.value = ReadingListsSelectionState(enabled = enabled)
    }

    fun toggleListSelection(listId: Long) {
        selectedArticleKeys.clear()
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
        val key = uiState.value.rows.filterIsInstance<ReadingListRow.PageRow>()
            .firstOrNull { it.page.id == pageId }?.articleKey ?: return
        if (!selectedArticleKeys.add(key)) {
            selectedArticleKeys.remove(key)
        }
        _selectionState.value = ReadingListsSelectionState(
            enabled = true,
            selectedArticleKeys = selectedArticleKeys.toSet()
        )
    }

    fun selectedPageIds() = selectionState.value.selectedArticleKeys.mapNotNullTo(mutableSetOf()) { uiState.value.articleIds[it] }

    fun selectAllLists() {
        selectedArticleKeys.clear()
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
        selectedArticleKeys.clear()
        uiState.value.rows.filterIsInstance<ReadingListRow.PageRow>()
            .mapTo(selectedArticleKeys) { it.articleKey }
        _selectionState.value = ReadingListsSelectionState(
            enabled = true,
            selectedArticleKeys = selectedArticleKeys.toSet()
        )
    }

    fun clearPageSelection() {
        selectedArticleKeys.clear()
        _selectionState.value = _selectionState.value.copy(selectedArticleKeys = emptySet())
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

    suspend fun loadReadingListWithPagesById(listId: Long): ReadingList? {
        return AppDatabase.instance.readingListDao().getListById(listId, true)
    }

    suspend fun loadReadingListsWithPagesByIds(listIds: Set<Long>): List<ReadingList> {
        val states = ArticleOfflineStateStore(AppDatabase.instance).snapshot()
        return AppDatabase.instance.readingListDao().getListsWithPagesByIds(listIds)
            .map { it.toReadingList().apply { pages.withOfflineState(states) } }
    }

    suspend fun getReadingListsWithoutContents(): List<ReadingList> {
        return AppDatabase.instance.readingListDao().getListsWithoutContents()
    }

    suspend fun createReadingList(title: String, description: String) {
        AppDatabase.instance.readingListDao().createList(title, description)
    }

    suspend fun getPageById(pageId: Long): ReadingListPage? =
        AppDatabase.instance.readingListPageDao().getActivePageWithOfflineState(pageId)

    suspend fun getPagesByIds(pageIds: Set<Long>): List<ReadingListPage> {
        return pageIds.toList().chunked(900).flatMap { AppDatabase.instance.readingListPageDao().getPagesByIds(it.toSet()) }
    }

    suspend fun getPageTitlesForIds(pageIds: Set<Long>): List<PageTitle> = getPagesByIds(pageIds)
        .distinctBy { it.articleKey() }
        .map { ReadingListPage.toPageTitle(it) }

    suspend fun prepareSelectedPagesForDeletion(pageIds: Set<Long> = selectedPageIds()): SelectedPagesDeletion? {
        val selectedPages = getPagesByIds(pageIds).filter { it.status != ReadingListPage.STATUS_QUEUE_FOR_DELETE }
        val selectedPageKeys = selectedPages.mapTo(mutableSetOf()) { it.articleKey() }
        if (selectedPageKeys.isEmpty()) return null
        val currentPages = AppDatabase.instance.readingListPageDao().getActivePages()
            .filter { it.articleKey() in selectedPageKeys }
        val containingListIds = currentPages.mapTo(mutableSetOf()) { it.listId }
        if (containingListIds.isEmpty()) {
            return null
        }

        val readingLists = mutableListOf<ReadingList>()
        val relations = AppDatabase.instance.readingListDao().getListsWithPagesByIds(containingListIds)
        relations.forEach { relation ->
            val pagesToDelete = relation.pages.filter { page ->
                page.status != ReadingListPage.STATUS_QUEUE_FOR_DELETE &&
                    page.articleKey() in selectedPageKeys
            }
            if (pagesToDelete.isNotEmpty()) {
                relation.list.pages.clear()
                relation.list.pages.addAll(pagesToDelete)
                readingLists.add(relation.list)
            }
        }

        return SelectedPagesDeletion(
            selectedPageCount = selectedPageKeys.size,
            readingLists = readingLists
        )
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
        allLists: MutableList<ReadingList>,
        tab: SavedTab,
        query: String?,
        articleFilter: SavedArticleFilter,
        recentPreviewSavedId: Long?,
        sortMode: Int
    ): List<ReadingListRow> {
        if (!query.isNullOrEmpty()) {
            return buildSearchRows(allLists, tab, query, articleFilter, recentPreviewSavedId)
        }

        return when (tab) {
            SavedTab.COLLECTIONS -> buildCollectionsRows(allLists, recentPreviewSavedId, sortMode)
            SavedTab.ALL_ARTICLES -> buildArticleRows(allLists, articleFilter = articleFilter, sortMode = sortMode)
        }
    }

    private fun buildCollectionsRows(
        lists: MutableList<ReadingList>,
        recentPreviewSavedId: Long?,
        sortMode: Int
    ): List<ReadingListRow> {
        ReadingList.sort(lists, sortMode)
        lists.removeAll { it.isDefault }
        return lists.map { ReadingListRow.ListRow(it.toUiModel(recentPreviewSavedId)) }
    }

    private fun buildSearchRows(
        lists: List<ReadingList>,
        tab: SavedTab,
        query: String,
        articleFilter: SavedArticleFilter,
        recentPreviewSavedId: Long?
    ): List<ReadingListRow> {
        val normalizedQuery = StringUtils.stripAccents(query)
        return when (tab) {
            SavedTab.COLLECTIONS -> lists
                .filter {
                    !it.isDefault &&
                        it.accentInvariantTitle.contains(normalizedQuery, ignoreCase = true)
                }
                .map { ReadingListRow.ListRow(it.toUiModel(recentPreviewSavedId)) }
            SavedTab.ALL_ARTICLES -> buildArticleRows(lists, normalizedQuery, articleFilter)
        }
    }

    /**
     * Every saved article across [lists] as [ReadingListRow.PageRow]s, de-duplicated by full article
     * identity so an article on multiple lists appears once, annotated with the lists that contain it.
     * When [titleFilter] is non-null, only articles whose title contains it (accent-insensitive) are kept.
     */
    private fun buildArticleRows(
        lists: List<ReadingList>,
        titleFilter: String? = null,
        articleFilter: SavedArticleFilter = SavedArticleFilter.ALL_ARTICLES,
        sortMode: Int? = null
    ): List<ReadingListRow.PageRow> {
        val selectedArticles = linkedMapOf<ArticleKey, ArticleRowData>()
        lists.forEach { list ->
            list.pages.forEach { page ->
                val matches = titleFilter == null ||
                    page.accentInvariantTitle.contains(titleFilter, ignoreCase = true)
                if (matches) {
                    val key = page.articleKey()
                    val current = selectedArticles[key]?.page
                    if (current == null || page.id < current.id) {
                        selectedArticles[key] = ArticleRowData(page)
                    }
                }
            }
        }

        if (selectedArticles.isEmpty()) {
            return emptyList()
        }

        lists.forEach { list ->
            val containingList = ContainingList(list.id, list.title, list.isDefault)
            list.pages.forEach { page ->
                selectedArticles[page.articleKey()]?.containingLists?.add(containingList)
            }
        }

        val filteredArticles = selectedArticles.values.filter { article ->
            articleFilter == SavedArticleFilter.ALL_ARTICLES ||
                article.containingLists.all { it.isDefault }
        }
        val orderedPages = filteredArticles.mapTo(mutableListOf()) { it.page }
        sortMode?.let { sortArticles(orderedPages, it) }

        return orderedPages.map { page ->
            val article = selectedArticles.getValue(page.articleKey())
            ReadingListRow.PageRow(
                page.toUiModel(),
                article.containingLists,
                page.articleKey()
            )
        }
    }

    private fun sortArticles(pages: MutableList<ReadingListPage>, sortMode: Int) {
        when (sortMode) {
            ReadingList.SORT_BY_NAME_ASC -> pages.sortWith { lhs: ReadingListPage, rhs: ReadingListPage -> lhs.accentInvariantTitle.compareTo(rhs.accentInvariantTitle, true) }
            ReadingList.SORT_BY_NAME_DESC -> pages.sortWith { lhs: ReadingListPage, rhs: ReadingListPage -> rhs.accentInvariantTitle.compareTo(lhs.accentInvariantTitle, true) }
            ReadingList.SORT_BY_RECENT_ASC -> pages.sortWith { lhs: ReadingListPage, rhs: ReadingListPage -> rhs.mtime.compareTo(lhs.mtime) }
            ReadingList.SORT_BY_RECENT_DESC -> pages.sortWith { lhs: ReadingListPage, rhs: ReadingListPage -> lhs.mtime.compareTo(rhs.mtime) }
        }
    }

    private fun ReadingList.toUiModel(recentPreviewSavedId: Long?): ReadingListUiModel {
        return ReadingListUiModel(
            id = id,
            title = title,
            description = description,
            isDefault = isDefault,
            totalPages = pages.size,
            numPagesOffline = numPagesOffline,
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
            offline = offlineRequested,
            saving = saving,
            availableOffline = availableOffline,
            isAvailable = WikipediaApp.instance.isOnline || availableOffline
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
    val numPagesOffline: Int = 0,
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
    val isAvailable: Boolean,
    val availableOffline: Boolean = offline && !saving
)

/**
 * A single row in the "Saved" list. Normally the screen only shows [ListRow]s; while searching it
 * shows matching [ListRow]s followed by matching [PageRow]s (articles found across all lists).
 */
sealed interface ReadingListRow {
    data class ListRow(val list: ReadingListUiModel) : ReadingListRow
    data class PageRow(val page: ReadingListPageUiModel, val containingLists: List<ContainingList>, val articleKey: ArticleKey) : ReadingListRow
}
data class ContainingList(val id: Long, val title: String, val isDefault: Boolean = false)

data class ReadingListsUiState(
    val isLoading: Boolean = true,
    val isSearchActive: Boolean = false,
    val rows: List<ReadingListRow> = emptyList(),
    val hasSavedArticles: Boolean = false,
    val pageDownloadProgress: Map<Long, Int> = emptyMap(),
    val articleIds: Map<ArticleKey, Long> = emptyMap(),
    val listCount: Int = 0,
    val searchQuery: String? = null,
    val selectedTab: SavedTab = SavedTab.ALL_ARTICLES,
    val selectedArticleFilter: SavedArticleFilter = SavedArticleFilter.ALL_ARTICLES,
    val sortMode: Int = ReadingList.SORT_BY_NAME_ASC,
    val showCollectionsBadge: Boolean = false,
    val onboarding: OnboardingState = OnboardingState.None,
    val discoverCard: RecommendedReadingListCard? = null,
    val pendingPreviewSavedListId: Long? = null
)

private data class ContentMode(
    val query: String?,
    val tab: SavedTab,
    val articleFilter: SavedArticleFilter
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
    val selectedArticleKeys: Set<ArticleKey> = emptySet()
)

data class SelectedPagesDeletion(
    val selectedPageCount: Int,
    val readingLists: List<ReadingList>
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
