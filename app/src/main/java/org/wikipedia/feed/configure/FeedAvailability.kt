package org.wikipedia.feed.configure

import com.google.gson.annotations.SerializedName

class FeedAvailability {

    @SerializedName("todays_featured_article")
    val featuredArticle: List<String>? = null
        get() = field ?: emptyList()

    @SerializedName("most_read")
    val mostRead: List<String>? = null
        get() = field ?: emptyList()

    @SerializedName("picture_of_the_day")
    val featuredPicture: List<String>? = null
        get() = field ?: emptyList()

    @SerializedName("in_the_news")
    val news: List<String>? = null
        get() = field ?: emptyList()

    @SerializedName("on_this_day")
    val onThisDay: List<String>? = null
        get() = field ?: emptyList()
}