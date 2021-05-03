package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite

class OnThisDayFunnel(app: WikipediaApp, wiki: WikiSite?, source: InvokeSource) :
        TimedFunnel(app, SCHEMA_NAME, REV_ID, SAMPLE_LOG_ALL, wiki) {

    private val source: Int = source.ordinal
    private var maxScrolledPosition = 0

    override fun preprocessSessionToken(eventData: JSONObject) {}

    fun scrolledToPosition(position: Int) {
        if (position > maxScrolledPosition) {
            maxScrolledPosition = position
        }
    }

    fun done(totalOnThisDayEvents: Int) {
        log(
                "source", source,
                "totalEvents", totalOnThisDayEvents,
                "scrolledEvents", maxScrolledPosition
        )
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppOnThisDay"
        private const val REV_ID = 18118721
    }
}
