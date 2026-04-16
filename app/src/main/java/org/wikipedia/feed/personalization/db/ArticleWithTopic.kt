package org.wikipedia.feed.personalization.db

import androidx.room.Embedded
import org.wikipedia.feed.personalization.db.entity.InterestArticle
import org.wikipedia.feed.personalization.db.entity.InterestTopic

data class ArticleWithTopic(
    @Embedded val article: InterestArticle,
    @Embedded(prefix = "topic_") val topic: InterestTopic
)
