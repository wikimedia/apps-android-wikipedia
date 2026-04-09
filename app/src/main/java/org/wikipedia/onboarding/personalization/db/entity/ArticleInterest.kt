package org.wikipedia.onboarding.personalization.db.entity

import androidx.room.Entity
import org.wikipedia.page.Namespace

@Entity(
    primaryKeys = ["apiTitle", "lang", "namespace"]
)
data class ArticleInterest(
    var apiTitle: String,
    val lang: String,
    val namespace: Namespace,
    var displayTitle: String,
    var description: String,
    var thumbUrl: String
)
