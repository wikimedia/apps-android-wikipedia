package org.wikipedia.categories.db

import androidx.room.Entity
import java.util.Date

@Entity(
    primaryKeys = ["title", "lang", "timeStamp"]
)
data class Category(
    val title: String,
    val lang: String,
    val timeStamp: Date = Date(),
)

data class CategoryCount(
    val title: String,
    val lang: String,
    val count: Long
)
