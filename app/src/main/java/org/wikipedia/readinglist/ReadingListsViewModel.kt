package org.wikipedia.readinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import org.apache.commons.lang3.StringUtils
import org.wikipedia.R
import org.wikipedia.auth.AccountUtil
import org.wikipedia.database.AppDatabase
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.readinglist.database.ReadingListWithPages
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig
class ReadingListsViewModel : ViewModel() {
    private val searchQuery = MutableStateFlow<String?>(null)
    private val searchActive = MutableStateFlow(false)
    private val selectionState = MutableStateFlow(ReadingListsSelectionState())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val uiState: StateFlow<ReadingListsUiState> =
        combine(
            AppDatabase.instance.readingListDao().getListsWithPagesFlow(),
            searchQuery,
            searchActive,
            selectionState,
            Prefs.observeKeys(
                R.string.preference_key_recommended_reading_list_onboarding_shown,
                R.string.preference_key_sync_reading_lists,
                R.string.preference_key_reading_list_sync_reminder_enabled,
                R.string.preference_key_reading_list_login_reminder_enabled,
                R.string.preference_key_reading_list_sort_mode
            )
        ) { relations, query, isSearchActive, selection, _ ->
            ReadingListsUiState(
                rows = buildRows(relations, query),
                searchQuery = query,
                onboarding = resolveOnboardingState(query, isSearchActive),
                isSelectionMode = selection.enabled,
                selectedListIds = selection.selectedListIds
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

    fun setSearchActive(active: Boolean) {
        searchActive.value = active
    }

    fun setRefreshing(refreshing: Boolean) {
        _isRefreshing.value = refreshing
    }

    fun setSelectionMode(enabled: Boolean) {
        selectionState.value = ReadingListsSelectionState(enabled = enabled)
    }

    fun toggleListSelection(listId: Long) {
        val selectedListIds = selectionState.value.selectedListIds.toMutableSet()
        if (!selectedListIds.add(listId)) {
            selectedListIds.remove(listId)
        }
        selectionState.value = ReadingListsSelectionState(
            enabled = true,
            selectedListIds = selectedListIds
        )
    }

    fun selectAllLists() {
        selectionState.value = ReadingListsSelectionState(
            enabled = true,
            selectedListIds = uiState.value.rows.filterIsInstance<ReadingListRow.ListRow>()
                .mapTo(mutableSetOf()) { it.list.id }
        )
    }

    fun clearListSelection() {
        selectionState.value = selectionState.value.copy(selectedListIds = emptySet())
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

    private fun resolveOnboardingState(query: String?, isSearchActive: Boolean): OnboardingState {
        if (isSearchActive || !query.isNullOrEmpty()) {
            return OnboardingState.None
        }
        return when {
            !Prefs.isRecommendedReadingListOnboardingShown -> {
                OnboardingState.RecommendedReadingList
            }
            (AccountUtil.isLoggedIn && !AccountUtil.isTemporaryAccount) && !Prefs.isReadingListSyncEnabled &&
                    Prefs.isReadingListSyncReminderEnabled && !RemoteConfig.config.disableReadingListSync -> {
                OnboardingState.SyncReminder
            }
            (!AccountUtil.isLoggedIn || AccountUtil.isTemporaryAccount) && Prefs.isReadingListLoginReminderEnabled &&
                    !RemoteConfig.config.disableReadingListSync -> {
                OnboardingState.LoginReminder
            }
            else -> OnboardingState.None
        }
    }

    private fun buildRows(relations: List<ReadingListWithPages>, query: String?): List<ReadingListRow> {
        // Room @Relation can't use where clause so we filter the pages queued for deletion here to match
        // then after that we sort, check for empty default list
        val lists = relations.map { relation ->
            relation.list.apply {
                pages.clear()
                pages.addAll(relation.pages.filterNot { it.status == ReadingListPage.STATUS_QUEUE_FOR_DELETE })
            }
        }.toMutableList()

        if (query.isNullOrEmpty()) {
            ReadingList.sort(lists, Prefs.getReadingListSortMode(ReadingList.SORT_BY_NAME_ASC))
            lists.removeEmptyDefaultList()
            return lists.map { ReadingListRow.ListRow(it.toUiModel()) }
        }

        return buildSearchRows(lists, query)
    }

    // matches lists first (in order), then matches pages across all lists
    // filters out duplicates by lang + apiTitle and also finds all titles a page is contained in for chips display.
    private fun buildSearchRows(lists: List<ReadingList>, query: String): List<ReadingListRow> {
        val normalizedQuery = StringUtils.stripAccents(query)
        val listRows = mutableListOf<ReadingListRow>()
        val pageRows = mutableListOf<ReadingListRow>()
        val seenPages = mutableSetOf<Pair<String, String>>()

        lists.forEach { list ->
            if (list.accentInvariantTitle.contains(normalizedQuery, ignoreCase = true)) {
                listRows.add(ReadingListRow.ListRow(list.toUiModel()))
            }
            list.pages.forEach { page ->
                if (page.accentInvariantTitle.contains(normalizedQuery, ignoreCase = true) &&
                    seenPages.add(page.lang to page.apiTitle)) {
                    pageRows.add(ReadingListRow.PageRow(page.toUiModel(), lists.findTitlesForPage(page)))
                }
            }
        }
        return listRows + pageRows
    }

    private fun List<ReadingList>.findTitlesForPage(page: ReadingListPage): List<String> {
        return filter { list -> list.pages.any { it.lang == page.lang && it.apiTitle == page.apiTitle } }
            .map { it.title }
    }

    private fun MutableList<ReadingList>.removeEmptyDefaultList() {
        if (size == 1 && this[0].isDefault && this[0].pages.isEmpty()) {
            removeAt(0)
        }
    }

    private fun ReadingList.toUiModel(): ReadingListUiModel {
        return ReadingListUiModel(
            id = id,
            title = title,
            description = description,
            isDefault = isDefault,
            totalPages = pages.size,
            sizeBytesFromPages = sizeBytesFromPages,
            thumbUrls = pages.mapNotNull { it.thumbUrl }.filterNot { it.isEmpty() },
            // TODO migration: wire the "new import" indicator once recentPreviewSavedReadingList is ported.
            isNew = false
        )
    }

    private fun ReadingListPage.toUiModel(): ReadingListPageUiModel {
        return ReadingListPageUiModel(
            id = id,
            title = displayTitle,
            description = description,
            thumbUrl = thumbUrl,
            lang = lang,
            apiTitle = apiTitle,
            offline = offline
        )
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5000L
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
    val offline: Boolean
)

/**
 * A single row in the "Saved" list. Normally the screen only shows [ListRow]s; while searching it
 * shows matching [ListRow]s followed by matching [PageRow]s (articles found across all lists).
 */
sealed interface ReadingListRow {
    data class ListRow(val list: ReadingListUiModel) : ReadingListRow
    data class PageRow(val page: ReadingListPageUiModel, val containingLists: List<String>) : ReadingListRow
}

data class ReadingListsUiState(
    val rows: List<ReadingListRow> = emptyList(),
    val searchQuery: String? = null,
    val onboarding: OnboardingState = OnboardingState.None,
    val isSelectionMode: Boolean = false,
    val selectedListIds: Set<Long> = emptySet()
)

private data class ReadingListsSelectionState(
    val enabled: Boolean = false,
    val selectedListIds: Set<Long> = emptySet()
)

sealed interface OnboardingState {
    data object None : OnboardingState
    data object RecommendedReadingList : OnboardingState
    data object SyncReminder : OnboardingState
    data object LoginReminder : OnboardingState
}
