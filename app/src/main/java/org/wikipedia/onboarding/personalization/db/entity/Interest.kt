package org.wikipedia.onboarding.personalization.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Interests")
data class Interest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: Int, // 0 = topic, 1 = article, use InterestType enum for better readability
    val lang: String,
    val topicLabel: String? = null,
    val topicKey: String? = null,
    var articleApiTitle: String? = null,
    var articleDisplayTitle: String? = null,
    var articleDescription: String? = null,
    var articleThumbUrl: String? = null,
)

enum class InterestType(val value: Int) {
    TOPIC(0),
    ARTICLE(1);

    companion object {
        fun fromValue(value: Int) = entries.first { it.value == value }
    }
}
