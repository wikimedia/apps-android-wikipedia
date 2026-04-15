package org.wikipedia.feed.personalization.db.entity

import androidx.room.Entity

@Entity(
    primaryKeys = ["topicId", "lang"]
)
data class InterestTopic(
    val topicId: String,
    val lang: String,
    val topicLabel: String,
    val queryTopicId: String
)
