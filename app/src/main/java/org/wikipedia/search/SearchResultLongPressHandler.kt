package org.wikipedia.search

import org.wikipedia.history.HistoryEntry
import org.wikipedia.readinglist.LongPressMenu

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

    override fun onSaveRequest(entry: HistoryEntry) {
        callback?.onSearchSavePage(entry)
    }
}
