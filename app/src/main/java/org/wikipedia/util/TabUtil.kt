package org.wikipedia.util

import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageBackStackItem
import org.wikipedia.page.tabs.Tab

object TabUtil {

    fun openInNewBackgroundTab(entry: HistoryEntry) {
        val app = WikipediaApp.getInstance()
        val tab = if (app.tabCount == 0) app.tabList[0] else Tab()
        if (app.tabCount > 0) {
            app.tabList.add(0, tab)
            while (app.tabList.size > Constants.MAX_TABS) {
                app.tabList.removeAt(0)
            }
        }
        tab.backStack.add(PageBackStackItem(entry.title, entry))
        app.commitTabState()
    }
}
