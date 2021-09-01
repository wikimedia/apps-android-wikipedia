package org.wikipedia.feed.aggregated

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.image.FeaturedImage
import org.wikipedia.feed.news.NewsItem
import org.wikipedia.feed.onthisday.OnThisDay
import org.wikipedia.feed.topread.TopRead

@Serializable
class AggregatedFeedContent {
    val tfa: PageSummary? = null
    val news: List<NewsItem>? = null
    @SerialName("mostread") val topRead: TopRead? = null
    @SerialName("image") val potd: FeaturedImage? = null
    val onthisday: List<OnThisDay.Event>? = null
}
