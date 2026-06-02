package org.wikipedia.feed.news

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.model.Card
import org.wikipedia.page.PageTitle

class NewsLinkCard(
    private val page: PageSummary,
    private val wiki: WikiSite,
) : Card() {

    fun pageTitle(): PageTitle {
        return page.getPageTitle(wiki)
    }
}
