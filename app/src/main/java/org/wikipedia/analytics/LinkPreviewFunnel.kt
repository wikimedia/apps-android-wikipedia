package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.Prefs

class LinkPreviewFunnel(app: WikipediaApp, private val source: Int) : TimedFunnel(app, SCHEMA_NAME, REV_ID, SAMPLE_LOG_ALL) {

    private var pageId = 0

    override fun preprocessData(eventData: JSONObject): JSONObject {
        preprocessData(eventData, "version", PROD_LINK_PREVIEW_VERSION)
        preprocessData(eventData, "source", source)
        preprocessData(eventData, "page_id", pageId)
        return super.preprocessData(eventData)
    }

    fun setPageId(pageId: Int) {
        this.pageId = pageId
    }

    fun logLinkClick() {
        log("action", "linkclick")
    }

    fun logNavigate() {
        log("action", if (Prefs.isLinkPreviewEnabled()) "navigate" else "disabled")
    }

    fun logCancel() {
        log("action", "cancel")
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppLinkPreview"
        private const val REV_ID = 18531254
        private const val PROD_LINK_PREVIEW_VERSION = 3
    }
}
