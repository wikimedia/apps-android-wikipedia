package org.wikipedia.feed.personalization.db.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable

@Entity(
    primaryKeys = ["topicId"]
)
@Serializable
data class InterestTopic(
    val topicId: String
)
