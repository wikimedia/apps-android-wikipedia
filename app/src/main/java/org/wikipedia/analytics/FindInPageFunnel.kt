package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite

class FindInPageFunnel(app: WikipediaApp, wiki: WikiSite?, private val pageId: Int) : TimedFunnel(app, SCHEMA_NAME, REV_ID, Funnel.Companion.SAMPLE_LOG_ALL, wiki) {
    private var pageHeight = 0
    private var numFindNext = 0
    private var numFindPrev = 0
    private var findText: String? = null
    override fun preprocessSessionToken(eventData: JSONObject) {}
    fun setPageHeight(height: Int) {
        pageHeight = height
    }

    fun addFindNext() {
        numFindNext++
    }

    fun addFindPrev() {
        numFindPrev++
    }

    fun setFindText(text: String?) {
        findText = text
    }

    fun logDone() {
        log(
                "pageID", pageId,
                "numFindNext", numFindNext,
                "numFindPrev", numFindPrev,
                "findText", findText,
                "pageHeight", pageHeight
        )
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppFindInPage"
        private const val REV_ID = 19690671
    }
}
