package org.wikipedia.feed.topread

import android.net.Uri
import androidx.core.net.toUri
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.CardType

class TopReadItemCard(
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
        return page.thumbnailUrl?.toUri()
    }

    override fun type(): CardType {
        return CardType.MOST_READ_ITEM
    }

    val pageViews get() = page.views
    val viewHistory get() = page.viewHistory ?: emptyList()
    val pageTitle get() = page.getPageTitle(wiki)
}
