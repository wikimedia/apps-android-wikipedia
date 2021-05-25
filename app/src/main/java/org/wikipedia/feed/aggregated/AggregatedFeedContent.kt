package org.wikipedia.feed.aggregated

import com.google.gson.annotations.SerializedName
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.image.FeaturedImage
import org.wikipedia.feed.mostread.MostRead
import org.wikipedia.feed.news.NewsItem
import org.wikipedia.feed.onthisday.OnThisDay

class AggregatedFeedContent {
    val tfa: PageSummary? = null
    val news: List<NewsItem>? = null
    @SerializedName("mostread") val mostRead: MostRead? = null
    @SerializedName("image") val potd: FeaturedImage? = null
    val onthisday: List<OnThisDay.Event>? = null
}
