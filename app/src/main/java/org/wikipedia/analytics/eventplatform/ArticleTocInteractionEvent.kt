package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.auth.AccountUtil
import java.util.concurrent.TimeUnit

class ArticleTocInteractionEvent(private val pageId: Int,
                                 private var wikiDb: String,
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
        EventPlatformClient.submit(ArticleTocInteractionEventImpl(!AccountUtil.isLoggedIn, wikiDb, pageId, 0, numOpens, numSectionClicks, 0, totalOpenedSec, numSections))
    }

    @Suppress("unused")
    @Serializable
    @SerialName("/analytics/mobile_apps/android_article_toc_interaction/1.0.0")
    class ArticleTocInteractionEventImpl(@SerialName("is_anon") private val isAnon: Boolean,
                                         @SerialName("wiki_db") private var wikiDb: String,
                                         @SerialName("page_id") private var pageId: Int,
                                         @SerialName("num_peeks") private var numberOfPeeks: Int,
                                         @SerialName("num_opens") private var numberOfOpens: Int,
                                         @SerialName("num_section_clicks") private var numberOfSectionClicks: Int,
                                         @SerialName("total_peek_sec") private var totalPeekSec: Int,
                                         @SerialName("total_open_sec") private var totalOpenSec: Int,
                                         @SerialName("num_sections") private var numSections: Int) :
        MobileAppsEvent("android.article_toc_interaction")
}
