package org.wikipedia.notifications

import android.content.Context
import android.net.Uri
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.diff.ArticleEditDetailsActivity
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.LinkHandler
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.util.CustomTabsUtil
import org.wikipedia.util.log.L

class NotificationLinkHandler(context: Context) : LinkHandler(context) {

    var category: NotificationCategory? = null

    override fun onPageLinkClicked(anchor: String, linkText: String) {
        // ignore
    }

    override fun onMediaLinkClicked(title: PageTitle) {
        // ignore
    }

    override fun onDiffLinkClicked(title: PageTitle, revisionId: Long) {
        context.startActivity(ArticleEditDetailsActivity.newIntent(context, title, revisionId))
    }

    override lateinit var wikiSite: WikiSite

    override fun onInternalLinkClicked(title: PageTitle) {
        // Make sure the login-failed links are opened in the external browser
        if (category == NotificationCategory.LOGIN_FAIL) {
            onExternalLinkClicked(Uri.parse(title.uri))
            return
        }
        context.startActivity(PageActivity.newIntentForCurrentTab(context,
            HistoryEntry(title, HistoryEntry.SOURCE_NOTIFICATION), title))
    }

    override fun onExternalLinkClicked(uri: Uri) {
        try {
            CustomTabsUtil.openInCustomTab(context, uri.toString())
        } catch (e: Exception) {
            L.e(e)
        }
    }
}
