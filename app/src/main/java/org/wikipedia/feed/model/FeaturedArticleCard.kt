package org.wikipedia.feed.model

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.settings.homefeed.CommunityModuleType

open class FeaturedArticleCard(
    val page: PageSummary,
    val age: Int, wiki: WikiSite,
) : WikiSiteCard(wiki) {

    override fun moduleKey(): String {
        return CommunityModuleType.FEATURED_ARTICLE.name
    }

    override fun dismissHashCode(): Int {
        return page.apiTitle.hashCode()
    }
}
