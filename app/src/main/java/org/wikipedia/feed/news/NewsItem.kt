package org.wikipedia.feed.news

import android.net.Uri
import org.wikipedia.Constants
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.util.ImageUrlUtil.getUrlForPreferredSize
import java.util.*

class NewsItem {

    val story: String? = null
        get() = field.orEmpty()
    val links: List<PageSummary?> = emptyList()

    fun linkCards(wiki: WikiSite?): List<NewsLinkCard> {
        val linkCards: MutableList<NewsLinkCard> = ArrayList()
        for (link in links) {
            if (link == null) {
                continue
            }
            linkCards.add(NewsLinkCard(link, wiki!!))
        }
        return linkCards
    }

    fun thumb(): Uri? {
        val uri = getFirstImageUri(links)
        return if (uri != null) Uri.parse(
            getUrlForPreferredSize(
                uri.toString(),
                Constants.PREFERRED_CARD_THUMBNAIL_SIZE
            )
        ) else null
    }

    private fun getFirstImageUri(links: List<PageSummary?>): Uri? {
        for (link in links) {
            if (link == null) {
                continue
            }
            val thumbnail = link.thumbnailUrl
            if (thumbnail != null) {
                return Uri.parse(thumbnail)
            }
        }
        return null
    }
}
