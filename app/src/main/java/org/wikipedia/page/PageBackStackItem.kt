package org.wikipedia.page

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import org.wikipedia.history.HistoryEntry
import java.util.Date

@Serializable
@Entity
class PageBackStackItem(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val apiTitle: String,
    val displayTitle: String,
    val langCode: String,
    val namespace: String,
    val timestamp: Long = Date().time,
    var scrollY: Int = 0
) {
    @Ignore
    var title: PageTitle? = null

    @Ignore
    var historyEntry: HistoryEntry? = null
}