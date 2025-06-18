package org.wikipedia.categories.db

import androidx.room.Entity

@Entity(
    primaryKeys = ["year", "month", "title", "lang"]
)
data class Category(
    val year: Int,
    val month: Int,
    val title: String,
    val lang: String,
    val count: Int
)
