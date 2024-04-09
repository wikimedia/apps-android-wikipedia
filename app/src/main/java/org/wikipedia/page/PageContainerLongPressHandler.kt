package org.wikipedia.page

import org.wikipedia.Constants.InvokeSource
import org.wikipedia.LongPressHandler.WebViewMenuCallback
import org.wikipedia.history.HistoryEntry
import org.wikipedia.readinglist.ReadingListBehaviorsUtil
import org.wikipedia.readinglist.database.ReadingListPage

class PageContainerLongPressHandler(private val fragment: PageFragment) : WebViewMenuCallback {

    override fun onOpenLink(entry: HistoryEntry) {
        fragment.loadPage(entry.title, entry)
    }

    override fun onOpenInNewTab(entry: HistoryEntry) {
        fragment.openInNewBackgroundTab(entry.title, entry)
    }

    override fun onAddRequest(entry: HistoryEntry, addToDefault: Boolean) {
        ReadingListBehaviorsUtil.addToDefaultList(fragment.requireActivity(), entry.title, addToDefault, InvokeSource.CONTEXT_MENU)
    }

    override fun onMoveRequest(page: ReadingListPage?, entry: HistoryEntry) {
        page?.run {
            ReadingListBehaviorsUtil.moveToList(fragment.requireActivity(), this.listId, entry.title, InvokeSource.CONTEXT_MENU)
        }
    }

    override val wikiSite = fragment.title?.wikiSite

    override val referrer = fragment.title?.uri
}
