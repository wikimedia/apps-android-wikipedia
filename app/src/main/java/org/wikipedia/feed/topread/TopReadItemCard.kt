package org.wikipedia.feed.topread

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.model.Card

class TopReadItemCard(
    private val page: PageSummary,
    private val wiki: WikiSite,
) : Card() {

    override fun title(): String {
        return page.displayTitle
    }

    val pageTitle get() = page.getPageTitle(wiki)
}
