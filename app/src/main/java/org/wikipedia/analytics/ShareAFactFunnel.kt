package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.WikipediaApp
import org.wikipedia.page.PageTitle

class ShareAFactFunnel(app: WikipediaApp, pageTitle: PageTitle, private val pageId: Int, private val revisionId: Long) :
        Funnel(app, SCHEMA_NAME, REV_ID, pageTitle.wikiSite) {

    private val pageTitle = pageTitle.displayText

    override fun preprocessData(eventData: JSONObject): JSONObject {
        preprocessData(eventData, "tutorial_feature_enabled", true)
        preprocessData(eventData, "tutorial_shown", 0)
        return super.preprocessData(eventData)
    }

    private fun logAction(action: String, text: String?, shareMode: ShareMode? = null) {
        val textStr = text.orEmpty().substring(0, MAX_LENGTH.coerceAtMost(text.orEmpty().length))
        log(
                "action", action,
                "article", pageTitle,
                "page_id", pageId,
                "rev_id", revisionId,
                "text", textStr,
                "share_mode", shareMode
        )
    }

    /** Text in the web view was highlighted.  */
    fun logHighlight() {
        logAction("highlight", "")
    }

    /** The share button in the UI was tapped.  */
    fun logShareTap(text: String) {
        logAction("sharetap", text)
    }

    /** 'Share as image' or 'Share as text' was tapped.  */
    fun logShareIntent(text: String, shareMode: ShareMode?) {
        logAction("shareintent", text, shareMode)
    }

    /**
     * 'Share as text' and 'Share as image' was shown but cancelled and neither was chosen.
     */
    fun logAbandoned(text: String) {
        logAction("abandoned", text)
    }

    enum class ShareMode {
        IMAGE, TEXT
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppShareAFact"
        private const val REV_ID = 18144594

        /**
         * The length value of 99 is somewhat arbitrary right now. We need to restrict the
         * total length of the event data somewhat to avoid the event getting dropped.
         */
        private const val MAX_LENGTH = 99
    }
}
