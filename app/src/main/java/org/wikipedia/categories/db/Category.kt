package org.wikipedia.categories.db

import androidx.room.Entity
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    primaryKeys = ["title", "lang"]
)
data class Category(
    val title: String,
    val lang: String,
    val count: Long,
    val date: Long = System.currentTimeMillis()
)

// for the API response
@Serializable
data class CategoryDto(
    val ns: Int,
    val title: String
)