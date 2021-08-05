package org.wikipedia.feed.announcement

import android.text.TextUtils
import org.wikipedia.util.DateUtil.iso8601DateParse
import com.google.gson.annotations.SerializedName
import org.apache.commons.lang3.StringUtils
import org.wikipedia.feed.announcement.Announcement
import org.wikipedia.json.annotations.Required
import java.util.*

class Announcement {

    @Required
    var id: String? = null

    @Required
    val type: String? = null

    @SerializedName("start_time")
    @Required
    private val startTime: String? = null

    @SerializedName("end_time")
    @Required
    private val endTime: String? = null
    val platforms = emptyList<String>()
    private val countries = emptyList<String>()

    @SerializedName("caption_HTML")
    private val footerCaption: String? = null

    @SerializedName("image_url")
    private var imageUrl: String? = null

    @SerializedName("image_height")
    private val imageHeight: String? = null

    @SerializedName("logged_in")
    private val loggedIn: Boolean? = null

    @SerializedName("reading_list_sync_enabled")
    private val readingListSyncEnabled: Boolean? = null
    private val beta: Boolean? = null
    private val border: Boolean? = null
    private val placement: String? = null

    @SerializedName("min_version")
    private val minVersion: String? = null

    @SerializedName("max_version")
    private val maxVersion: String? = null

    @Required
    private var text: String? = null
    private var action: Action? = null

    @SerializedName("negative_text")
    private var negativeText: String? = null

    constructor() {}
    constructor(id: String, text: String, imageUrl: String, action: Action, negativeText: String) {
        this.id = id
        this.text = text
        this.imageUrl = imageUrl
        this.action = action
        this.negativeText = negativeText
    }

    fun startTime(): Date {
        return iso8601DateParse(startTime!!)
    }

    fun endTime(): Date {
        return iso8601DateParse(endTime!!)
    }

    fun countries(): List<String> {
        return countries
    }

    fun text(): String {
        return text.orEmpty()
    }

    fun hasAction(): Boolean {
        return action != null
    }

    fun actionTitle(): String {
        return if (action != null) action!!.title() else ""
    }

    fun actionUrl(): String {
        return if (action != null) action!!.url() else ""
    }

    fun hasFooterCaption(): Boolean {
        return !TextUtils.isEmpty(footerCaption)
    }

    fun footerCaption(): String {
        return StringUtils.defaultString(footerCaption)
    }

    fun hasImageUrl(): Boolean {
        return !TextUtils.isEmpty(imageUrl)
    }

    fun imageUrl(): String {
        return StringUtils.defaultString(imageUrl)
    }

    fun imageHeight(): String {
        return StringUtils.defaultString(imageHeight)
    }

    fun negativeText(): String? {
        return negativeText
    }

    fun loggedIn(): Boolean? {
        return loggedIn
    }

    fun readingListSyncEnabled(): Boolean? {
        return readingListSyncEnabled
    }

    fun beta(): Boolean? {
        return beta
    }

    fun placement(): String {
        return StringUtils.defaultString(placement, PLACEMENT_FEED)
    }

    fun hasBorder(): Boolean {
        return border != null && border
    }

    fun minVersion(): String? {
        return minVersion
    }

    fun maxVersion(): String? {
        return maxVersion
    }

    class Action(@field:Required private val title: String,
                 @field:Required private val url: String) {
        fun title(): String {
            return title
        }

        fun url(): String {
            return url
        }
    }

    companion object {
        const val SURVEY = "survey"
        const val FUNDRAISING = "fundraising"
        const val PLACEMENT_FEED = "feed"
        const val PLACEMENT_ARTICLE = "article"
    }
}
