package org.wikipedia.feed.topread

import android.net.Uri
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.CardType

class TopReadItemCard internal constructor(private val page: TopReadArticles,
                                           private val wiki: WikiSite) : Card() {

    override fun title(): String {
        return page.displayTitle
    }

    override fun subtitle(): String? {
        return page.description
    }

    override fun image(): Uri? {
        return if (page.thumbnailUrl.isNullOrEmpty()) null else Uri.parse(page.thumbnailUrl)
    }

    override fun type(): CardType {
        return CardType.MOST_READ_ITEM
    }

    val pageViews get() = page.views
    val viewHistory get() = page.viewHistory
    val pageTitle get() = page.getPageTitle(wiki)
}
