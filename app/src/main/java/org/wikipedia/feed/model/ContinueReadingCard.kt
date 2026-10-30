package org.wikipedia.feed.model

import kotlinx.serialization.Serializable
import org.wikipedia.page.PageTitle

@Serializable
class ContinueReadingCard(
    val title: PageTitle,
    val source: Int
) : ForYouCard() {
    override fun dismissHashCode(): Int {
        return title.hashCode()
    }
}
