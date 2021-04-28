package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.WikipediaApp

class AppLanguageSearchingFunnel(private val settingsSessionToken: String) :
        TimedFunnel(WikipediaApp.getInstance(), SCHEMA_NAME, REV_ID, SAMPLE_LOG_ALL) {
    fun logLanguageAdded(languageAdded: Boolean, languageCode: String?, searchString: String?) {
        log(
                "language_settings_token", settingsSessionToken,
                "added", languageAdded,
                "language", languageCode,
                "search_string", searchString
        )
    }

    fun logNoLanguageAdded(languageAdded: Boolean, searchString: String?) {
        log(
                "language_settings_token", settingsSessionToken,
                "added", languageAdded,
                "search_string", searchString
        )
    }

    override fun preprocessSessionToken(eventData: JSONObject) {}

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppLanguageSearching"
        private const val REV_ID = 18204518
    }
}
