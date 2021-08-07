package org.wikipedia.feed.configure

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class FeedAvailability(
    @Json(name = "todays_featured_article")
    val featuredArticle: List<String> = emptyList(),

    @Json(name = "most_read")
    val mostRead: List<String> = emptyList(),

    @Json(name = "picture_of_the_day")
    val featuredPicture: List<String> = emptyList(),

    @Json(name = "in_the_news")
    val news: List<String> = emptyList(),

    @Json(name = "on_this_day")
    val onThisDay: List<String> = emptyList(),
)
