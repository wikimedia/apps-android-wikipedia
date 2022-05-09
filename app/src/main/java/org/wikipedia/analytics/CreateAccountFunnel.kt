package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.WikipediaApp

class CreateAccountFunnel(app: WikipediaApp, private val requestSource: String) : Funnel(app, SCHEMA_NAME, REVISION) {

    fun logStart(loginSessionToken: String?) {
        log("action" to "start", "loginSessionToken" to loginSessionToken)
    }

    fun logError(code: String?) {
        log("action" to "error", "errorText" to code)
    }

    fun logSuccess() {
        log("action" to "success")
    }

    override fun preprocessData(eventData: JSONObject): JSONObject {
        preprocessData(eventData, "source", requestSource)
        return super.preprocessData(eventData)
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppCreateAccount"
        private const val REVISION = 20709917
    }
}
