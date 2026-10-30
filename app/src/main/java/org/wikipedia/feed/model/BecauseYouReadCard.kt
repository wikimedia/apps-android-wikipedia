package org.wikipedia.feed.model

import kotlinx.serialization.Serializable
import org.wikipedia.page.PageTitle

@Serializable
class BecauseYouReadCard(
    val title: PageTitle,
    val sourceDisplayTitle: String
) : ForYouCard() {
    override fun dismissHashCode(): Int {
        return title.hashCode()
    }
}
