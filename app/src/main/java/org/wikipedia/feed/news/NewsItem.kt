package org.wikipedia.feed.news

import android.net.Uri
import org.wikipedia.Constants
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.util.ImageUrlUtil.getUrlForPreferredSize

class NewsItem {

    val story: String = ""
    val links: List<PageSummary?> = emptyList()

    fun linkCards(wiki: WikiSite): List<NewsLinkCard> {
        val linkCards = mutableListOf<NewsLinkCard>()
        links.filterNotNull().map { NewsLinkCard(it, wiki) }
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
        links.filterNotNull().map { pageSummary ->
            pageSummary.thumbnailUrl?.let { return Uri.parse(it) }
        }
        return null
    }
}
