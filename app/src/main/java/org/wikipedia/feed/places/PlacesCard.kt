package org.wikipedia.feed.places

import org.wikipedia.R
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.NearbyPage
import org.wikipedia.feed.model.CardType
import org.wikipedia.feed.model.WikiSiteCard
import org.wikipedia.util.DateUtil
import org.wikipedia.util.L10nUtil

class PlacesCard(wiki: WikiSite,
                 val age: Int,
                 val nearbyPage: NearbyPage? = null) : WikiSiteCard(wiki) {

    override fun type(): CardType {
        return CardType.PLACES
    }

    override fun title(): String {
        return L10nUtil.getString(wikiSite().languageCode, R.string.places_card_title)
    }

    override fun subtitle(): String {
        return DateUtil.getFeedCardDateString(age)
    }

    fun footerActionText(): String {
        return L10nUtil.getString(wikiSite().languageCode, R.string.places_card_action)
    }
}
