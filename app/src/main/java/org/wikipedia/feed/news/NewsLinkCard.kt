package org.wikipedia.feed.news

import android.net.Uri
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.CardType
import org.wikipedia.page.PageTitle
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.ImageUrlUtil.getUrlForPreferredSize

class NewsLinkCard(private val page: PageSummary, private val wiki: WikiSite) : Card() {

    override fun title(): String {
        return page.displayTitle
    }

    override fun subtitle(): String? {
        return page.description
    }

    override fun image(): Uri? {
        val thumbUrl = page.thumbnailUrl
        return if (thumbUrl.isNullOrEmpty()) null else Uri.parse(ImageUrlUtil.getUrlForPreferredSize(thumbUrl, Service.PREFERRED_THUMB_SIZE))
    }

    override fun type(): CardType {
        return CardType.NEWS_ITEM_LINK
    }

    fun pageTitle(): PageTitle {
        return page.getPageTitle(wiki)
    }
}
