package org.wikipedia.feed.announcement

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
class Announcement(
    val id: String = "",
    val text: String = "",
    val type: String = "",
    val platforms: List<String> = emptyList(),
    val countries: List<String> = emptyList(),
    @Json(name = "start_time") val startTime: Date? = null,
    @Json(name = "end_time") val endTime: Date? = null,
    @Json(name = "image_url") val imageUrl: String = "",
    @Json(name = "negative_text") val negativeText: String = "",
    @Json(name = "caption_HTML") val footerCaption: String = "",
    @Json(name = "image_height") val imageHeight: String = "",
    @Json(name = "logged_in") val isLoggedIn: Boolean? = null,
    @Json(name = "reading_list_sync_enabled") val isReadingListSyncEnabled: Boolean? = null,
    @Json(name = "min_version") val minVersion: String = "",
    @Json(name = "max_version") val maxVersion: String = "",
    val beta: Boolean? = null,
    val border: Boolean = false,
    val placement: String = PLACEMENT_FEED,
    val action: Action? = null
) {
    val actionTitle: String
        get() = action?.title ?: ""
    val actionUrl: String
        get() = action?.url ?: ""
    val hasAction: Boolean
        get() = action != null
    val hasFooterCaption: Boolean
        get() = footerCaption.isNotEmpty()
    val hasImageUrl: Boolean
        get() = imageUrl.isNotEmpty()

    @JsonClass(generateAdapter = true)
    class Action(val title: String, val url: String)

    companion object {
        const val SURVEY = "survey"
        const val FUNDRAISING = "fundraising"
        const val PLACEMENT_FEED = "feed"
        const val PLACEMENT_ARTICLE = "article"
    }
}
