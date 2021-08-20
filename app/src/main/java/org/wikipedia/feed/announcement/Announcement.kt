package org.wikipedia.feed.announcement

import com.google.gson.annotations.SerializedName
import org.wikipedia.util.DateUtil
import java.util.*

class Announcement {

    private var action: Action? = null

    @SerializedName("negative_text")
    var negativeText: String? = null

    var id: String = ""

    @SerializedName("start_time")
    private val startTime: String? = null

    @SerializedName("end_time")
    private val endTime: String? = null
    private val border: Boolean? = null

    var text: String? = null

    val type: String = ""

    @SerializedName("image_url")
    var imageUrl: String? = null

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

    constructor()

    constructor(id: String?,
                text: String?,
                imageUrl: String?,
                action: Action?,
                negativeText: String?) {
        this.id = id.orEmpty()
        this.text = text.orEmpty()
        this.imageUrl = imageUrl.orEmpty()
        this.action = action
        this.negativeText = negativeText
    }

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
        return action?.title ?: ""
    }

    fun actionUrl(): String {
        return action?.url ?: ""
    }

    fun hasFooterCaption(): Boolean {
        return footerCaption.isNotEmpty()
    }

    fun hasImageUrl(): Boolean {
        return !imageUrl.isNullOrEmpty()
    }

    fun hasBorder(): Boolean {
        return border == true
    }

    class Action(val title: String, val url: String)

    companion object {
        const val SURVEY = "survey"
        const val FUNDRAISING = "fundraising"
        const val PLACEMENT_FEED = "feed"
        const val PLACEMENT_ARTICLE = "article"
    }
}
