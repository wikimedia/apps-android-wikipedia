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

    fun execAction(action: Action) {
        when (action) {
            Action.SWIPE_FORWARD -> numSwipesForward++
            Action.SWIPE_BACK ->    numSwipesBack++
            Action.CLICK_FORWARD -> numClicksForward++
            Action.CLICK_BACK ->    numClicksBack++
        }
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

    enum class Action {
        SWIPE_FORWARD,
        SWIPE_BACK,
        CLICK_FORWARD,
        CLICK_BACK,
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppRandomizer"
        private const val REV_ID = 18118733
    }
}
