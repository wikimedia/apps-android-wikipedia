package org.wikipedia.page

import org.wikipedia.Constants
import org.wikipedia.LongPressHandler.WebViewMenuCallback
import org.wikipedia.history.HistoryEntry
import org.wikipedia.readinglist.database.ReadingListPage

class PageContainerLongPressHandler(private val fragment: PageFragment) : WebViewMenuCallback {

    override fun onOpenLink(entry: HistoryEntry) {
        fragment.loadPage(entry.title, entry)
    }

    override fun onOpenInNewTab(entry: HistoryEntry) {
        fragment.openInNewBackgroundTab(entry.title, entry)
    }

    override fun onAddRequest(entry: HistoryEntry, addToDefault: Boolean) {
        fragment.addToReadingList(entry.title, Constants.InvokeSource.CONTEXT_MENU, addToDefault)
    }

    override fun onMoveRequest(page: ReadingListPage?, entry: HistoryEntry) {
        page?.run {
            fragment.moveToReadingList(listId, entry.title, Constants.InvokeSource.CONTEXT_MENU, true)
        }
    }

    override val wikiSite = fragment.title?.wikiSite

    override val referrer = fragment.title?.uri
}
