package org.wikipedia.feed.interests

import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.CardType
import org.wikipedia.feed.personalization.db.entity.InterestArticle
import org.wikipedia.feed.personalization.db.entity.InterestTopic
import org.wikipedia.history.HistoryEntry

class BasedOnInterestCard(
    val entry: HistoryEntry,
    val interestTopic: InterestTopic? = null,
    val interestArticle: InterestArticle? = null
) : Card() {
    override fun type(): CardType {
        return CardType.RANDOM // TODO: remove, since this is no longer used
    }

    override fun dismissHashCode(): Int {
        return entry.title.hashCode()
    }
}
