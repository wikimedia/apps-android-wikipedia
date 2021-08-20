package org.wikipedia.feed.announcement

import com.google.gson.annotations.SerializedName
import org.wikipedia.util.DateUtil
import java.util.*

class Announcement(val id: String = "",
                   val text: String = "",
                   val type: String = "",
                   val platforms: List<String> = emptyList(),
                   val countries: List<String> = emptyList(),
                   @SerializedName("start_time") private val startTime: String = "",
                   @SerializedName("end_time") private val endTime: String = "",
                   @SerializedName("image_url") val imageUrl: String? = "",
                   @SerializedName("negative_text") val negativeText: String? = "",
                   @SerializedName("caption_HTML") val footerCaption: String? = "",
                   @SerializedName("image_height") val imageHeight: String? = "",
                   @SerializedName("logged_in") val loggedIn: Boolean? = null,
                   @SerializedName("reading_list_sync_enabled") val readingListSyncEnabled: Boolean? = null,
                   @SerializedName("min_version") val minVersion: String? = null,
                   @SerializedName("max_version") val maxVersion: String? = null,
                   val border: Boolean? = null,
                   val beta: Boolean? = null,
                   val placement: String = PLACEMENT_FEED,
                   val action: Action?) {


    fun startTime(): Date {
        return DateUtil.iso8601DateParse(startTime)
    }

    fun endTime(): Date {
        return DateUtil.iso8601DateParse(endTime)
    }

    fun hasAction(): Boolean {
        return action != null
    }

    fun actionTitle(): String {
        return action?.title ?: ""
    }

    fun actionUrl(): String {
        return action?.url ?: ""
    }

    fun hasFooterCaption(): Boolean {
        return !footerCaption.isNullOrEmpty()
    }

    fun hasImageUrl(): Boolean {
        return !imageUrl.isNullOrEmpty()
    }

    class Action(val title: String, val url: String)

    companion object {
        const val SURVEY = "survey"
        const val FUNDRAISING = "fundraising"
        const val PLACEMENT_FEED = "feed"
        const val PLACEMENT_ARTICLE = "article"
    }
}
