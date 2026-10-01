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

class PageOverflowMenuViewModel : ViewModel() {
    data class PageOverflowMenuState(
        val entry: HistoryEntry,
        val isArticleSaved: Boolean,
        val menuKey: String
    )

    var pageOverflowMenuState by mutableStateOf<PageOverflowMenuState?>(null)

    fun onPageOverflowClick(
        wikiSite: WikiSite,
        pageSummary: PageSummary,
        source: Int,
        menuKey: String
    ) {
        viewModelScope.launch {
            val entry = pageSummary.getHistoryEntry(wikiSite, source)
            val isArticleSaved = AppDatabase.instance.readingListPageDao().findPageInAnyList(entry.title) != null
            pageOverflowMenuState = PageOverflowMenuState(
                entry = entry,
                isArticleSaved = isArticleSaved,
                menuKey = menuKey
            )
        }
    }

    fun dismissPageOverflowMenu() {
        pageOverflowMenuState = null
    }
}
