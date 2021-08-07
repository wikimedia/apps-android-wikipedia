package org.wikipedia.feed.aggregated

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.image.FeaturedImage
import org.wikipedia.feed.news.NewsItem
import org.wikipedia.feed.onthisday.OnThisDay
import org.wikipedia.feed.topread.TopRead

@JsonClass(generateAdapter = true)
class AggregatedFeedContent(
    val tfa: PageSummary? = null,
    val news: List<NewsItem> = emptyList(),
    @Json(name = "mostread") val topRead: TopRead? = null,
    @Json(name = "image") val potd: FeaturedImage? = null,
    @Json(name = "onthisday") val onThisDay: List<OnThisDay.Event> = emptyList()
)
