package org.wikipedia.search.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
class RecentSearch constructor(
    @PrimaryKey val text: String,
    val timestamp: Date = Date())
