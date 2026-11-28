package org.wikipedia.compose.components.menu

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.history.HistoryEntry
import org.wikipedia.readinglist.database.ReadingList

class PageOverflowMenuViewModel : ViewModel() {
    data class PageOverflowMenuState(
        val entry: HistoryEntry,
        val lists: List<ReadingList>,
        val menuKey: String
    )

    var pageOverflowMenuState by mutableStateOf<PageOverflowMenuState?>(null)

    fun onPageOverflowClick(
        wikiSite: WikiSite,
        pageSummary: PageSummary,
        source: Int,
        menuKey: String
    ) {
        onPageOverflowClick(pageSummary.getHistoryEntry(wikiSite, source), menuKey)
    }

    fun onPageOverflowClick(entry: HistoryEntry, menuKey: String) {
        viewModelScope.launch {
            val lists = AppDatabase.instance.readingListDao().getListsFromPageOccurrences(
                AppDatabase.instance.readingListPageDao().getAllPageOccurrences(entry.title)
            )
            pageOverflowMenuState = PageOverflowMenuState(
                entry = entry,
                lists = lists,
                menuKey = menuKey
            )
        }
    }

    fun dismissPageOverflowMenu() {
        pageOverflowMenuState = null
    }
}
