package org.wikipedia.categories.db

import androidx.room.Entity
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    primaryKeys = ["title", "lang", "timeStamp"]
)
data class Category(
    val title: String,
    val lang: String,
    val timeStamp: Long = System.currentTimeMillis(),
)

// for the API response
@Serializable
data class CategoryResponse(
    val ns: Int,
    val title: String
)

data class CategoryCount(
    val title: String,
    val lang: String,
    val count: Long
)
