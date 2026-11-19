package org.wikipedia.page

import org.wikipedia.LongPressHandler.WebViewMenuCallback
import org.wikipedia.history.HistoryEntry

class PageContainerLongPressHandler(private val fragment: PageFragment) : WebViewMenuCallback {

    override fun onOpenLink(entry: HistoryEntry) {
        fragment.loadPage(entry.title, entry)
    }

    override fun onOpenInNewTab(entry: HistoryEntry) {
        fragment.openInNewBackgroundTab(entry.title, entry)
    }

    override fun onSaveRequest(entry: HistoryEntry) {
        // TODO: replace with the old onAddRequest / onMoveRequest.
    }

    override val wikiSite = fragment.title?.wikiSite

    override val referrer = fragment.title?.uri

    override val historyEntryId get() = fragment.model.curEntry?.id ?: -1
}
