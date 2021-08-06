package org.wikipedia.feed.announcement

import com.google.gson.annotations.SerializedName
import org.wikipedia.json.annotations.Required
import org.wikipedia.util.DateUtil
import java.util.*

class Announcement(id: String,
                   @Required var text: String,
                   @SerializedName("image_url") var imageUrl: String,
                   action: Action,
                   negativeText: String) {

    private var action: Action? = action

    @SerializedName("negative_text")
    var negativeText: String? = negativeText

    @Required
    var id: String = ""

    @SerializedName("start_time")
    @Required
    private val startTime: String? = null

    @SerializedName("end_time")
    @Required
    private val endTime: String? = null
    private val border: Boolean? = null

    @Required
    val type: String = ""

    @SerializedName("caption_HTML")
    val footerCaption: String = ""

    @SerializedName("image_height")
    val imageHeight: String = ""

    @SerializedName("logged_in")
    val loggedIn: Boolean? = null

    @SerializedName("reading_list_sync_enabled")
    val readingListSyncEnabled: Boolean? = null

    @SerializedName("min_version")
    val minVersion: String? = null

    @SerializedName("max_version")
    val maxVersion: String? = null

    val beta: Boolean? = null
    val platforms = emptyList<String>()
    val countries = emptyList<String>()
    val placement: String = PLACEMENT_FEED

    fun startTime(): Date {
        return DateUtil.iso8601DateParse(startTime!!)
    }

    fun endTime(): Date {
        return DateUtil.iso8601DateParse(endTime!!)
    }

    fun hasAction(): Boolean {
        return action != null
    }

    fun actionTitle(): String {
        return if (action != null) action!!.title else ""
    }

    fun actionUrl(): String {
        return if (action != null) action!!.url else ""
    }

    fun hasFooterCaption(): Boolean {
        return footerCaption.isNotEmpty()
    }

    fun hasImageUrl(): Boolean {
        return imageUrl.isNotEmpty()
    }

    fun hasBorder(): Boolean {
        return border != null && border
    }

    class Action(@field:Required val title: String, @field:Required val url: String)

    companion object {
        const val SURVEY = "survey"
        const val FUNDRAISING = "fundraising"
        const val PLACEMENT_FEED = "feed"
        const val PLACEMENT_ARTICLE = "article"
    }
}
