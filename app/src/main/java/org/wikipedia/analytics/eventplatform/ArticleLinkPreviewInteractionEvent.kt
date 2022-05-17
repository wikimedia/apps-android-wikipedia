package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.settings.Prefs

class ArticleLinkPreviewInteractionEvent(private val wikiDb: String,
                                         private val pageId: Int,
                                         private val source: Int) : TimedEvent() {

    fun logLinkClick() {
        submitEvent("linkclick")
    }

    fun logNavigate() {
        submitEvent(if (Prefs.isLinkPreviewEnabled) "navigate" else "disabled")
    }

    fun logCancel() {
        submitEvent("cancel")
    }

    private fun submitEvent(action: String) {
        EventPlatformClient.submit(ArticleLinkPreviewInteractionEventImpl(action, source, duration, wikiDb, pageId, PROD_LINK_PREVIEW_VERSION))
    }

    @Suppress("unused")
    @Serializable
    @SerialName("/analytics/mobile_apps/android_article_link_preview_interaction/1.0.0")
    class ArticleLinkPreviewInteractionEventImpl(private val action: String,
                                                 private val source: Int,
                                                 @SerialName("time_spent_ms") private val timeSpentMs: Int,
                                                 @SerialName("wiki_db") private val wikiDb: String,
                                                 @SerialName("page_id") private val pageId: Int,
                                                 private val version: Int) :
        MobileAppsEvent("android.article_link_preview_interaction")

    companion object {
        private const val PROD_LINK_PREVIEW_VERSION = 3
    }
}
