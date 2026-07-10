package org.wikipedia.readinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import org.apache.commons.lang3.StringUtils
import org.wikipedia.database.AppDatabase
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.readinglist.database.ReadingListWithPages
import org.wikipedia.settings.Prefs
class ReadingListsViewModel : ViewModel() {
    private val searchQuery = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ReadingListsUiState> =
        combine(
            AppDatabase.instance.readingListDao().getListsWithPagesFlow(),
            searchQuery
        ) { relations, query ->
            ReadingListsUiState(
                content = ReadingListsUiState.Content.Success(buildRows(relations, query)),
                searchQuery = query
            )
        }
            .catch { emit(ReadingListsUiState(content = ReadingListsUiState.Content.Error(it))) }
            .flowOn(Dispatchers.IO)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = ReadingListsUiState(content = ReadingListsUiState.Content.Loading)
            )

    fun setSearchQuery(query: String?) {
        searchQuery.value = query
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
            numPages = pages.size,
            sizeBytes = sizeBytesFromPages
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
 * Immutable snapshot of a reading list for rendering, used for [ReadingListRow.ListRow]
 * and [ReadingListsUiState.Content.Success].
 */
data class ReadingListUiModel(
    val id: Long,
    val title: String,
    val description: String?,
    val isDefault: Boolean,
    val numPages: Int,
    val sizeBytes: Long
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

/**
 * UI state for the [ReadingListsComposeScreen].
 * [Content] represents the core data state, which can only be Loading, Success, or Error.
 * The outer class wraps [Content] so we can safely add independent UI states on top
 * (like a search query or a pull-to-refresh spinner) without overriding the core data.
 */
data class ReadingListsUiState(
    val content: Content,
    val searchQuery: String? = null
) {
    sealed interface Content {
        data object Loading : Content
        data class Success(val rows: List<ReadingListRow>) : Content
        data class Error(val throwable: Throwable) : Content
    }
}
