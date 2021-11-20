package org.wikipedia.search.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity
class RecentSearch constructor(
    @PrimaryKey val text: String,
    val timestamp: Instant = Instant.now()
)
