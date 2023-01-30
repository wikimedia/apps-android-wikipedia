package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class ArticleFindInPageInteractionEvent(private val pageId: Int) :
    TimedEvent() {

    private var numFindNext = 0
    private var numFindPrev = 0
    var pageHeight = 0
    var findText = ""

    fun addFindNext() {
        numFindNext++
    }

    fun addFindPrev() {
        numFindPrev++
    }

    fun logDone() {
        EventPlatformClient.submit(ArticleFindInPageInteractionEventImpl(pageId, findText, numFindNext, numFindPrev, pageHeight, duration))
    }

    @Suppress("unused")
    @Serializable
    @SerialName("/analytics/mobile_apps/android_find_in_page_interaction/1.0.0")
    class ArticleFindInPageInteractionEventImpl(@SerialName("page_id") private val pageId: Int,
                                                @SerialName("find_text") private val findText: String,
                                                @SerialName("find_next_clicks_count") private val findNextClicksCount: Int,
                                                @SerialName("find_prev_clicks_count") private val findPrevClicksCount: Int,
                                                @SerialName("page_height") private val pageHeight: Int,
                                                @SerialName("time_spent_ms") private val timeSpentMs: Int) :
        MobileAppsEvent("android.find_in_page_interaction")
}
