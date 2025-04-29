package org.wikipedia.categories.db

import androidx.room.Entity

@Entity(
    primaryKeys = ["title", "lang", "timeStamp"]
)
data class Category(
    val title: String,
    val lang: String,
    val timeStamp: Long = System.currentTimeMillis(),
)

data class CategoryCount(
    val title: String,
    val lang: String,
    val count: Long
)
