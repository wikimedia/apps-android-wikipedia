package org.wikipedia.feed.topread

import com.google.gson.annotations.SerializedName
import org.wikipedia.dataclient.page.PageSummary
import java.util.*

class TopReadArticles : PageSummary() {

    private val rank = 0
    val views = 0

    @SerializedName("view_history")
    val viewHistory: List<ViewHistory>? = null

    inner class ViewHistory {
        val date: Date? = null
        val views = 0f
    }
}
