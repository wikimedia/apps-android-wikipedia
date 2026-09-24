package org.wikipedia.feed.model

import kotlinx.serialization.Serializable
import org.wikipedia.feed.personalization.db.entity.InterestTopic
import org.wikipedia.page.PageTitle

@Serializable
class NewWithinInterestCard(
    val titles: List<PageTitle>,
    val interestTopic: InterestTopic
) : ForYouCard() {

    override fun dismissHashCode(): Int {
        return titles.sumOf { it.hashCode() }
    }
}
