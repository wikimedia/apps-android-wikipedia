package org.wikipedia.feed.announcement

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.wikipedia.util.DateUtil
import java.util.*

@Serializable
class Announcement(val id: String = "",
                   val text: String = "",
                   val type: String = "",
                   val platforms: List<String> = emptyList(),
                   val countries: List<String> = emptyList(),
                   @SerialName("start_time") private val startTime: String? = null,
                   @SerialName("end_time") private val endTime: String? = null,
                   @SerialName("image_url") val imageUrl: String? = "",
                   @SerialName("negative_text") val negativeText: String? = "",
                   @SerialName("caption_HTML") val footerCaption: String? = "",
                   @SerialName("image_height") val imageHeight: String? = "",
                   @SerialName("logged_in") val loggedIn: Boolean? = null,
                   @SerialName("reading_list_sync_enabled") val readingListSyncEnabled: Boolean? = null,
                   // The Min and Max version could be an integer for Android versions, or a string
                   // for iOS versions, so these need to be serialized manually.
                   @SerialName("min_version") private val minVersion: JsonElement? = null,
                   @SerialName("max_version") private val maxVersion: JsonElement? = null,
                   val border: Boolean? = null,
                   val beta: Boolean? = null,
                   val placement: String = PLACEMENT_FEED,
                   val action: Action?) {

    fun startTime(): Date? {
        return startTime?.let { DateUtil.iso8601DateParse(it) }
    }

    fun endTime(): Date? {
        return endTime?.let { DateUtil.iso8601DateParse(it) }
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

    fun minVersion(): Int {
        return minVersion?.jsonPrimitive?.intOrNull ?: -1
    }

    fun maxVersion(): Int {
        return maxVersion?.jsonPrimitive?.intOrNull ?: -1
    }

    @Serializable
    class Action(val title: String, val url: String)

    companion object {
        const val SURVEY = "survey"
        const val FUNDRAISING = "fundraising"
        const val PLACEMENT_FEED = "feed"
        const val PLACEMENT_ARTICLE = "article"
    }
}
