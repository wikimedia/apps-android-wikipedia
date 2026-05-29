package org.wikipedia.feed.model

import kotlinx.serialization.Serializable
import org.wikipedia.page.PageTitle

@Serializable
class PlacesOfInterestCard(
    val title: PageTitle
) : ForYouCard() {
    override fun type(): CardType {
        return CardType.PLACES
    }

    override fun dismissHashCode(): Int {
        return title.hashCode()
    }
}
