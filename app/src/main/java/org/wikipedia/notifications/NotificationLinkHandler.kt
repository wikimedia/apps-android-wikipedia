package org.wikipedia.notifications

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.LinkHandler
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.util.log.L

class NotificationLinkHandler constructor(context: Context) : LinkHandler(context) {

    override fun onPageLinkClicked(anchor: String, linkText: String) {
        // ignore
    }

    override fun onMediaLinkClicked(title: PageTitle) {
        // ignore
    }

    override lateinit var wikiSite: WikiSite

    override fun onInternalLinkClicked(title: PageTitle) {
        context.startActivity(PageActivity.newIntentForCurrentTab(context,
            HistoryEntry(title, HistoryEntry.SOURCE_NOTIFICATION), title))
    }

    override fun onExternalLinkClicked(uri: Uri) {
        try {
            // TODO: handle "change password" since it will open a blank page in PageActivity
            context. startActivity(Intent(Intent.ACTION_VIEW).setData(uri))
        } catch (e: Exception) {
            L.e(e)
        }
    }
}
