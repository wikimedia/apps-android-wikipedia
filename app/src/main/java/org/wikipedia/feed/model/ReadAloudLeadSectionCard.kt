package org.wikipedia.feed.model

import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.personalization.db.entity.InterestTopic

@Serializable
class ReadAloudLeadSectionCard(
    val summary: PageSummary,
    val interestTopic: InterestTopic
) : ForYouCard() {
    override fun dismissHashCode(): Int {
        return summary.apiTitle.hashCode() + summary.lang.hashCode()
    }
}
