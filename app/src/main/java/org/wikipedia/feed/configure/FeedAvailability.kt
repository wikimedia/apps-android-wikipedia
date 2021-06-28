package org.wikipedia.feed.configure

import com.google.gson.annotations.SerializedName

class FeedAvailability {

    @SerializedName("todays_featured_article")
    val featuredArticle: List<String> = emptyList()

    @SerializedName("most_read")
    val mostRead: List<String> = emptyList()

    @SerializedName("picture_of_the_day")
    val featuredPicture: List<String> = emptyList()

    @SerializedName("in_the_news")
    val news: List<String> = emptyList()

    @SerializedName("on_this_day")
    val onThisDay: List<String> = emptyList()
}
