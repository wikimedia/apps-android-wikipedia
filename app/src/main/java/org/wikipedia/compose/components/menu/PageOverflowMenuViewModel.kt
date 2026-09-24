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
        onSaveRequest: (HistoryEntry) -> Unit,
        onShareRequest: (HistoryEntry) -> Unit,
        onLinkCopyRequest: (HistoryEntry) -> Unit
    ) {
        viewModelScope.launch {
            val entry = pageSummary.getHistoryEntry(wikiSite, source)
            val isArticleSaved = AppDatabase.instance.readingListPageDao().findPageInAnyList(entry.title) != null
            pageOverflowMenuState = PageOverflowMenuState(
                entry = entry,
                items = buildOverflowMenuItems(
                    context = context,
                    entry = entry,
                    isArticleSaved = isArticleSaved,
                    onOpenPage = onOpenPage,
                    onOpenInNewTab = onOpenInNewTab,
                    onSaveRequest = onSaveRequest,
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
        isArticleSaved: Boolean,
        onOpenPage: (HistoryEntry) -> Unit = {},
        onOpenInNewTab: (HistoryEntry) -> Unit = {},
        onSaveRequest: (HistoryEntry) -> Unit,
        onShareRequest: (HistoryEntry) -> Unit,
        onLinkCopyRequest: (HistoryEntry) -> Unit
    ): List<Pair<String, () -> Unit>> = buildList {

        add(context.getString(R.string.menu_long_press_open_page) to { onOpenPage(entry) })
        add(context.getString(R.string.menu_long_press_open_in_new_tab) to { onOpenInNewTab(entry) })

        // A single entry either way: both states open the save sheet, which is where adding to and
        // removing from collections now happens.
        val saveLabel = if (isArticleSaved) {
            context.getString(R.string.link_preview_dialog_saved_button)
        } else {
            context.getString(R.string.feed_card_add_to_default_list)
        }
        add(saveLabel to { onSaveRequest(entry) })

        add(context.getString(R.string.menu_page_share) to { onShareRequest(entry) })
        add(context.getString(R.string.menu_long_press_copy_page) to { onLinkCopyRequest(entry) })
    }
}
