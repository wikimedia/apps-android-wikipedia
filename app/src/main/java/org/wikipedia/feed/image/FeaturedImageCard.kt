package org.wikipedia.feed.image

import android.net.Uri
import org.wikipedia.R
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.model.CardType
import org.wikipedia.feed.model.WikiSiteCard
import org.wikipedia.util.DateUtil
import org.wikipedia.util.L10nUtil

class FeaturedImageCard(private val featuredImage: FeaturedImage,
                        private val age: Int,
                        wiki: WikiSite) : WikiSiteCard(wiki) {

    override fun title(): String {
        return L10nUtil.getStringForArticleLanguage(wikiSite().languageCode(), R.string.view_featured_image_card_title)
    }

    override fun subtitle(): String {
        return DateUtil.getFeedCardDateString(age)
    }

    override fun image(): Uri? {
        return if (featuredImage.thumbnailUrl.isEmpty()) null else Uri.parse(featuredImage.thumbnailUrl)
    }

    override fun type(): CardType {
        return CardType.FEATURED_IMAGE
    }

    override fun dismissHashCode(): Int {
        return featuredImage.title.hashCode()
    }

    fun baseImage(): FeaturedImage {
        return featuredImage
    }

    fun age(): Int {
        return age
    }

    fun filename(): String {
        return featuredImage.title
    }

    fun description(): String {
        return featuredImage.description.text
    }
}
