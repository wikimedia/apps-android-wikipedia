package org.wikipedia.feed.configure

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.util.ReleaseUtil

@Serializable
class FeedAvailability {

    @SerialName("todays_featured_article")
    val featuredArticle: List<String> = emptyList()

    @SerialName("most_read")
    val mostRead: List<String> = emptyList()

    @SerialName("picture_of_the_day")
    val featuredPicture: List<String> = emptyList()

    @SerialName("in_the_news")
    val news: List<String> = emptyList()

    @SerialName("on_this_day")
    val onThisDay: List<String> = emptyList()

    @SerialName("games")
    val games: List<String> = if (ReleaseUtil.isPreBetaRelease) listOf("en", "de") else listOf("de")
}
