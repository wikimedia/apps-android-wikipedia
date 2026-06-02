package org.wikipedia.feed.places

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.NearbyPage
import org.wikipedia.feed.model.WikiSiteCard

class PlacesCard(
    wiki: WikiSite,
    val age: Int,
    val nearbyPage: NearbyPage? = null
) : WikiSiteCard(wiki) {
    // TODO: add dismissHashCode and moduleKey when the card is ready to be added to the feed.
}
