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

    override fun subtitle(): String? {
        return page.description
    }

    val pageTitle get() = page.getPageTitle(wiki)
}
