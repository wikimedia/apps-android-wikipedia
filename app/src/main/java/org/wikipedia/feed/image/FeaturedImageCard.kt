package org.wikipedia.feed.image

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.model.WikiSiteCard
import org.wikipedia.settings.homefeed.CommunityModuleType

class FeaturedImageCard(
    val featuredImage: FeaturedImage,
    val age: Int,
    wiki: WikiSite
) : WikiSiteCard(wiki) {

    override fun moduleKey(): String {
        return CommunityModuleType.FEATURED_IMAGE.name
    }

    override fun dismissHashCode(): Int {
        return featuredImage.title.hashCode()
    }
}
