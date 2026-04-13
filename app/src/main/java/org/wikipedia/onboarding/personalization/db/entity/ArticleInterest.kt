package org.wikipedia.onboarding.personalization.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import org.wikipedia.page.Namespace

@Entity(
    primaryKeys = ["apiTitle", "lang", "namespace"],
    foreignKeys = [
        ForeignKey(
            entity = TopicInterest::class,
            parentColumns = ["topicId", "lang"], // primary key in the parent entity
            childColumns = ["topicId", "topicLang"], // foreign key in this entity which references the primary key in parent entity
            onDelete = ForeignKey.SET_NULL, // when a topic is deleted, the foreign key in this entity will be set to null to not delete the article interest but just disassociate it from the deleted topic
        )
    ],
    indices = [Index(value = ["topicId", "topicLang"])] // index for the foreign key columns to improve query performance especially for cascade operations
)
data class ArticleInterest(
    val apiTitle: String,
    val lang: String,
    val namespace: Namespace,
    val displayTitle: String,
    val description: String,
    val thumbUrl: String,
    // foreign key referencing the topicId, lang in TopicInterest
    val topicId: String? = null,
    val topicLang: String? = null
)
