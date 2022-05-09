package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.WikipediaApp

class IntentFunnel(app: WikipediaApp) : Funnel(app, SCHEMA_NAME, REV_ID) {

    fun logSearchWidgetTap() {
        log("action" to "searchwidgettap")
    }

    fun logFeaturedArticleWidgetTap() {
        log("action" to "featuredarticlewidgettap")
    }

    fun logShareIntent() {
        log("action" to "share")
    }

    fun logProcessTextIntent() {
        log("action" to "processtext")
    }

    override fun preprocessSessionToken(eventData: JSONObject) {}

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppIntents"
        private const val REV_ID = 18115555
    }
}
