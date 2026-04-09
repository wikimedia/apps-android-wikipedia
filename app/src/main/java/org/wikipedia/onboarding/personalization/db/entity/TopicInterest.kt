package org.wikipedia.onboarding.personalization.db.entity

import androidx.room.Entity

@Entity(
    primaryKeys = ["topicId", "lang"]
)
data class TopicInterest(
    val topicId: String,
    val lang: String,
    val topicLabel: String,
    val queryTopicId: String
)
