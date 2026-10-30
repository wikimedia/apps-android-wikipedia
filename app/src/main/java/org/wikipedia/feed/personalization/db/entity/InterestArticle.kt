package org.wikipedia.feed.personalization.db.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable
import org.wikipedia.page.Namespace

@Entity(
    primaryKeys = ["apiTitle", "lang", "namespace"]
)
@Serializable
data class InterestArticle(
    val apiTitle: String,
    val lang: String,
    val namespace: Namespace,
    val displayTitle: String,
    val description: String,
    val thumbUrl: String
)
