package org.wikipedia.compose.components.menu

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.history.HistoryEntry
import org.wikipedia.readinglist.database.ReadingList

class PageOverflowMenuViewModel : ViewModel() {
    data class PageOverflowMenuState(
        val entry: HistoryEntry,
        val items: List<Pair<String, () -> Unit>>,
        val menuKey: String
    )

    var pageOverflowMenuState by mutableStateOf<PageOverflowMenuState?>(null)

    fun onPageOverflowClick(
        context: Context,
        wikiSite: WikiSite,
        pageSummary: PageSummary,
        source: Int,
        menuKey: String,
        onOpenPage: (HistoryEntry) -> Unit = {},
        onOpenInNewTab: (HistoryEntry) -> Unit = {},
        onAddRequest: (HistoryEntry, addToDefault: Boolean) -> Unit,
        onMoveRequest: (Long, HistoryEntry) -> Unit,
        onRemoveRequest: (HistoryEntry, List<ReadingList>) -> Unit,
        onShareRequest: (HistoryEntry) -> Unit,
        onLinkCopyRequest: (HistoryEntry) -> Unit
    ) {
        viewModelScope.launch {
            val entry = pageSummary.getHistoryEntry(wikiSite, source)
            val lists = AppDatabase.instance.readingListDao().getListsFromPageOccurrences(
                AppDatabase.instance.readingListPageDao().getAllPageOccurrences(entry.title)
            )
            pageOverflowMenuState = PageOverflowMenuState(
                entry = entry,
                items = buildOverflowMenuItems(
                    context = context,
                    entry = entry,
                    lists = lists,
                    onOpenPage = onOpenPage,
                    onOpenInNewTab = onOpenInNewTab,
                    onAddRequest = onAddRequest,
                    onMoveRequest = onMoveRequest,
                    onRemoveRequest = onRemoveRequest,
                    onShareRequest = onShareRequest,
                    onLinkCopyRequest = onLinkCopyRequest
                ),
                menuKey = menuKey
            )
        }
    }

    fun dismissPageOverflowMenu() {
        pageOverflowMenuState = null
    }

    private fun buildOverflowMenuItems(
        context: Context,
        entry: HistoryEntry,
        lists: List<ReadingList>,
        onOpenPage: (HistoryEntry) -> Unit = {},
        onOpenInNewTab: (HistoryEntry) -> Unit = {},
        onAddRequest: (HistoryEntry, addToDefault: Boolean) -> Unit,
        onMoveRequest: (Long, HistoryEntry) -> Unit,
        onRemoveRequest: (HistoryEntry, List<ReadingList>) -> Unit,
        onShareRequest: (HistoryEntry) -> Unit,
        onLinkCopyRequest: (HistoryEntry) -> Unit
    ): List<Pair<String, () -> Unit>> = buildList {

        add(context.getString(R.string.menu_long_press_open_page) to { onOpenPage(entry) })
        add(context.getString(R.string.menu_long_press_open_in_new_tab) to { onOpenInNewTab(entry) })

        if (lists.isEmpty()) {
            add(context.getString(R.string.feed_card_add_to_default_list) to { onAddRequest(entry, true) })
        } else {
            add(context.getString(R.string.reading_list_add_to_other_list) to { onAddRequest(entry, false) })

            val removeLabel = if (lists.size == 1) {
                context.getString(R.string.reading_list_remove_from_list, lists[0].title)
            } else {
                context.getString(R.string.reading_list_remove_from_lists)
            }
            add(removeLabel to { onRemoveRequest(entry, lists) })

            if (lists.size == 1) {
                add(context.getString(R.string.reading_list_move_from_to_other_list, lists[0].title) to {
                    onMoveRequest(lists[0].id, entry)
                })
            }
        }

        add(context.getString(R.string.menu_page_share) to { onShareRequest(entry) })
        add(context.getString(R.string.menu_long_press_copy_page) to { onLinkCopyRequest(entry) })
    }
}
