package org.wikipedia.feed.model

import kotlinx.serialization.Serializable
import org.wikipedia.feed.personalization.db.entity.InterestArticle
import org.wikipedia.feed.personalization.db.entity.InterestTopic
import org.wikipedia.page.PageTitle

@Serializable
class BasedOnInterestCard(
    val title: PageTitle,
    val interestTopic: InterestTopic? = null,
    val interestArticle: InterestArticle? = null
) : ForYouCard() {

    override fun dismissHashCode(): Int {
        return title.hashCode()
    }
}
