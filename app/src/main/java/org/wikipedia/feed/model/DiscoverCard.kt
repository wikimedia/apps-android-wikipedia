package org.wikipedia.feed.model

import kotlinx.serialization.Serializable
import org.wikipedia.page.PageTitle

@Serializable
class DiscoverCard(
    val title: PageTitle
) : ForYouCard() {
    override fun dismissHashCode(): Int {
        return title.hashCode()
    }
}
