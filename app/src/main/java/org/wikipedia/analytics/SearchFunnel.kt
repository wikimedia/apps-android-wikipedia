package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.WikipediaApp
import org.wikipedia.json.JsonUtil

class SearchFunnel(app: WikipediaApp, private val source: InvokeSource) :
        Funnel(app, SCHEMA_NAME, REVISION, SAMPLE_LOG_100) {

    fun searchStart() {
        log(
                "action", "start",
                "language", JsonUtil.encodeToString(app.language().appLanguageCodes)
        )
    }

    fun searchCancel(languageCode: String?) {
        log(
                "action", "cancel",
                "language", languageCode
        )
    }

    fun searchClick(position: Int, languageCode: String?) {
        log(
                "action", "click",
                "position", position,
                "language", languageCode
        )
    }

    fun searchDidYouMean(languageCode: String?) {
        log(
                "action", "didyoumean",
                "language", languageCode
        )
    }

    fun searchResults(fullText: Boolean, numResults: Int, delayMillis: Int, languageCode: String?) {
        log(
                "action", "results",
                "type_of_search", if (fullText) "full" else "prefix",
                "number_of_results", numResults,
                "time_to_display_results", delayMillis,
                "language", languageCode
        )
    }

    fun searchError(fullText: Boolean, delayMillis: Int, languageCode: String?) {
        log(
                "action", "error",
                "type_of_search", if (fullText) "full" else "prefix",
                "time_to_display_results", delayMillis,
                "language", languageCode
        )
    }

    fun searchLanguageSwitch(previousLanguage: String, currentLanguage: String) {
        if (previousLanguage != currentLanguage) {
            log(
                    "action", "langswitch",
                    "language", "$previousLanguage>$currentLanguage"
            )
        }
    }

    override fun preprocessData(eventData: JSONObject): JSONObject {
        preprocessData(eventData, "invoke_source", source.ordinal)
        return super.preprocessData(eventData)
    }

    companion object {
        /**
         * Please email someone in Discovery (Search Team's Product Manager or a Data Analyst)
         * if you change the schema name or version.
         */
        private const val SCHEMA_NAME = "MobileWikiAppSearch"
        private const val REVISION = 18204643
    }
}
