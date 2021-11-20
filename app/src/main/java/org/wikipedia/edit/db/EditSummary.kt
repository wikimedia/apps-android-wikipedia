package org.wikipedia.edit.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity
class EditSummary constructor(
    @PrimaryKey val summary: String,
    val lastUsed: Instant = Instant.now()
) {
    override fun toString(): String {
        return summary
    }
}
