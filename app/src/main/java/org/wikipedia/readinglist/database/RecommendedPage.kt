package org.wikipedia.readinglist.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.Namespace
import java.util.Date

@Entity
data class RecommendedPage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val wiki: WikiSite,
    val lang: String = "en",
    val namespace: Namespace,
    val timestamp: Date = Date(),
    var apiTitle: String,
    var displayTitle: String,
    var description: String? = null,
    var thumbUrl: String? = null,
    var status: Int = 0 // 0 = new, 1 = expired
)
