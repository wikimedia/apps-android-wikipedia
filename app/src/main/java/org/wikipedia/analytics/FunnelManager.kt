package org.wikipedia.analytics

import org.wikipedia.WikipediaApp
import org.wikipedia.page.PageTitle
import java.util.*

class FunnelManager(private val app: WikipediaApp) {

    private val editFunnels = Hashtable<PageTitle, EditFunnel>()

    fun getEditFunnel(title: PageTitle): EditFunnel? {
        if (!editFunnels.containsKey(title)) {
            editFunnels[title] = EditFunnel(app, title)
        }
        return editFunnels[title]
    }
}
