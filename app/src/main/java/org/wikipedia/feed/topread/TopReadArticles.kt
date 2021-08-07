package org.wikipedia.feed.topread

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.page.PageSummary
import java.util.*

@JsonClass(generateAdapter = true)
class TopReadArticles(
    val views: Int = 0,
    internal val rank: Int = 0,
    @Json(name = "view_history") val viewHistory: List<ViewHistory> = emptyList()
) : PageSummary() {
    @JsonClass(generateAdapter = true)
    class ViewHistory(val date: Date, val views: Float)
}
