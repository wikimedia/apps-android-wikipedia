package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.WikipediaApp
import org.wikipedia.page.PageTitle

class DescriptionEditFunnel(app: WikipediaApp, title: PageTitle, private val type: Type,
                            private val source: InvokeSource) : EditFunnel(app, title) {

    enum class Type(private val logString: String) {
        NEW("new"), EXISTING("existing");

        fun toLogString(): String {
            return logString
        }
    }

    override fun preprocessData(eventData: JSONObject): JSONObject {
        preprocessData(eventData, "source", source.value)
        return super.preprocessData(eventData)
    }

    override fun logStart() {
        log("action" to "start", "wikidataDescriptionEdit" to type.toLogString())
    }

    fun logReady() {
        log("action" to "ready", "wikidataDescriptionEdit" to type.toLogString())
    }

    override fun logSaveAttempt() {
        log("action" to "saveAttempt", "wikidataDescriptionEdit" to type.toLogString())
    }

    override fun logSaved(revID: Long) {
        log(
            "action" to "saved",
            "revID" to revID,
            "wikidataDescriptionEdit" to type.toLogString()
        )
    }

    override fun logAbuseFilterWarning(code: String?) {
        log(
            "action" to "abuseFilterWarning",
            "abuseFilterName" to code,
            "wikidataDescriptionEdit" to type.toLogString()
        )
    }

    override fun logError(code: String?) {
        log(
            "action" to "error",
            "errorText" to code,
            "wikidataDescriptionEdit" to type.toLogString()
        )
    }
}
