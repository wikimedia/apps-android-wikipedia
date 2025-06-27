package org.wikipedia.categories.db

import androidx.room.Entity
import java.time.LocalDate

@Entity(
    primaryKeys = ["year", "month", "title", "lang"]
)
data class Category(
    val year: Int,
    val month: Int,
    val title: String,
    val lang: String,
    val count: Int
) {
    constructor(title: String, lang: String) : this(
        year = LocalDate.now().year,
        month = LocalDate.now().monthValue,
        title = title,
        lang = lang,
        count = 1
    )
}
