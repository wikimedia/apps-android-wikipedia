package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import java.util.concurrent.TimeUnit

class ToCInteractionFunnel(app: WikipediaApp, wiki: WikiSite?, private val pageId: Int, private val numSections: Int) :
        Funnel(app, SCHEMA_NAME, REV_ID, SAMPLE_LOG_ALL, wiki) {

    private var numOpens = 0
    private var numSectionClicks = 0
    private var lastScrollStartMillis: Long = 0
    private var totalOpenedSec = 0

    override fun preprocessData(eventData: JSONObject): JSONObject? {
        preprocessData(eventData, "page_id", pageId)
        preprocessData(eventData, "num_sections", numSections)
        return super.preprocessData(eventData)
    }

    override fun preprocessSessionToken(eventData: JSONObject) {}

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

    fun log() {
        scrollStop()
        if (numSections == 0 || numOpens == 0) {
            return
        }
        log(
                "num_peeks", 0,
                "num_opens", numOpens,
                "num_section_clicks", numSectionClicks,
                "total_peek_sec", 0,
                "total_open_sec", totalOpenedSec
        )
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppToCInteraction"
        private const val REV_ID = 19044853
    }
}
