package org.wikipedia.feed.personalization.db.entity

import androidx.room.Entity

@Entity(
    primaryKeys = ["topicId"]
)
data class InterestTopic(
    val topicId: String
)
