package org.wikipedia.feed.news

import android.net.Uri
import androidx.core.net.toUri
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.CardType
import org.wikipedia.page.PageTitle
import org.wikipedia.util.ImageUrlUtil

class NewsLinkCard(
    private val page: PageSummary,
    private val wiki: WikiSite,
) : Card() {

    override fun title(): String {
        return page.displayTitle
    }

    override fun subtitle(): String? {
        return page.description
    }

    override fun image(): Uri? {
        return page.thumbnailUrl?.let {
            ImageUrlUtil.getUrlForPreferredSize(it, Service.PREFERRED_THUMB_SIZE).toUri()
        }
    }

    override fun type(): CardType {
        return CardType.NEWS_ITEM_LINK
    }

    fun pageTitle(): PageTitle {
        return page.getPageTitle(wiki)
    }
}
