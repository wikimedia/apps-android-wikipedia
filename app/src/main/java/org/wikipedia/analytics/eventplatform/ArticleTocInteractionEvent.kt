package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

class ArticleTocInteractionEvent(private val pageId: Int,
                                 private val wikiDb: String,
                                 private val numSections: Int) {

    private var numOpens = 0
    private var numSectionClicks = 0
    private var lastScrollStartMillis = 0L
    private var totalOpenedSec = 0

    fun scrollStart() {
        numOpens++
        lastScrollStartMillis = System.currentTimeMillis()
    }

    fun scrollStop() {
        if (lastScrollStartMillis == 0L) {
            return
        }
        totalOpenedSec += ((System.currentTimeMillis() - lastScrollStartMillis) / TimeUnit.SECONDS.toMillis(1)).toInt()
        lastScrollStartMillis = 0
    }

    fun logClick() {
        numSectionClicks++
    }

    fun logEvent() {
        scrollStop()
        if (numSections == 0 || numOpens == 0) {
            return
        }
        EventPlatformClient.submit(ArticleTocInteractionEventImpl(wikiDb, pageId, numOpens, numSectionClicks, totalOpenedSec, numSections))
    }

    @Suppress("unused")
    @Serializable
    @SerialName("/analytics/mobile_apps/android_article_toc_interaction/1.0.0")
    class ArticleTocInteractionEventImpl(@SerialName("wiki_db") private val wikiDb: String,
                                         @SerialName("page_id") private val pageId: Int,
                                         @SerialName("num_opens") private val numberOfOpens: Int,
                                         @SerialName("num_section_clicks") private val numberOfSectionClicks: Int,
                                         @SerialName("total_open_sec") private val totalOpenSec: Int,
                                         @SerialName("num_sections") private val numSections: Int) :
        MobileAppsEvent("android.article_toc_interaction")
}
