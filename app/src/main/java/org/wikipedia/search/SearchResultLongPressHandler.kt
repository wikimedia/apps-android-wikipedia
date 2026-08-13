package org.wikipedia.search

import org.wikipedia.history.HistoryEntry
import org.wikipedia.readinglist.LongPressMenu
import org.wikipedia.readinglist.database.ReadingListPage

class SearchResultLongPressHandler(
    private val callback: SearchResultCallback?,
    private val lastPositionRequested: Int
) :
    LongPressMenu.Callback {
    override fun onOpenLink(entry: HistoryEntry) {
        callback?.navigateToTitle(entry.title, false, lastPositionRequested)
    }

    override fun onOpenInNewTab(entry: HistoryEntry) {
        callback?.navigateToTitle(entry.title, true, lastPositionRequested)
    }

    override fun onAddRequest(entry: HistoryEntry, addToDefault: Boolean) {
        callback?.onSearchAddPageToList(entry, addToDefault)
    }

    override fun onMoveRequest(page: ReadingListPage?, entry: HistoryEntry) {
        page.let {
            callback?.onSearchMovePageToList(page!!.listId, entry)
        }
    }
}
