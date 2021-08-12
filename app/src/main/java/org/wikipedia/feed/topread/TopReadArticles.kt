package org.wikipedia.feed.topread

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.page.PageSummary
import java.util.*

@Serializable
class TopReadArticles : PageSummary() {

    private val rank = 0
    val views = 0

    @SerializedName("view_history")
    val viewHistory: List<@Contextual ViewHistory>? = null

    inner class ViewHistory {
        val date: Date? = null
        val views = 0f
    }
}
