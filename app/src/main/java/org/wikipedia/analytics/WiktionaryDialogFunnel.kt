package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.WikipediaApp
import org.wikipedia.util.ReleaseUtil

class WiktionaryDialogFunnel(app: WikipediaApp, private val text: String) :
        TimedFunnel(app, SCHEMA_NAME, REV_ID, if (ReleaseUtil.isProdRelease) SAMPLE_LOG_100 else SAMPLE_LOG_ALL) {

    fun logClose() {
        log("text", text)
    }

    override fun preprocessSessionToken(eventData: JSONObject) {}

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppWiktionaryPopup"
        private const val REV_ID = 18118768
    }
}
