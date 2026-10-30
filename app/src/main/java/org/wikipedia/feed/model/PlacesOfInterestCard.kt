package org.wikipedia.feed.model

import kotlinx.serialization.Serializable
import org.wikipedia.page.PageTitle

@Serializable
class PlacesOfInterestCard(
    val title: PageTitle,
    val distance: String
) : ForYouCard() {
    override fun dismissHashCode(): Int {
        return title.hashCode()
    }
}
