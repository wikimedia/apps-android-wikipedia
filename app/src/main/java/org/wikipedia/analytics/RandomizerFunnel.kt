package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite

class RandomizerFunnel(app: WikipediaApp, wiki: WikiSite?, private val source: InvokeSource) :
        TimedFunnel(app, SCHEMA_NAME, REV_ID, SAMPLE_LOG_ALL, wiki) {
    private var numSwipesForward = 0
    private var numSwipesBack = 0
    private var numClicksForward = 0
    private var numClicksBack = 0
    override fun preprocessSessionToken(eventData: JSONObject) {}
    fun swipedForward() {
        numSwipesForward++
    }

    fun swipedBack() {
        numSwipesBack++
    }

    fun clickedForward() {
        numClicksForward++
    }

    fun clickedBack() {
        numClicksBack++
    }

    fun done() {
        log(
                "source", source.ordinal,
                "fingerSwipesForward", numSwipesForward,
                "fingerSwipesBack", numSwipesBack,
                "diceClicks", numClicksForward,
                "backClicks", numClicksBack
        )
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppRandomizer"
        private const val REV_ID = 18118733
    }
}
