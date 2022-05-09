package org.wikipedia.analytics

import org.wikipedia.WikipediaApp

class LoginFunnel(app: WikipediaApp) : Funnel(app, SCHEMA_NAME, REVISION) {
    fun logStart(source: String = "", editSessionToken: String = "") {
        log("action" to "start", "source" to source, "edit_session_token" to editSessionToken)
    }

    fun logCreateAccountAttempt() {
        log("action" to "createAccountAttempt")
    }

    fun logCreateAccountFailure() {
        log("action" to "createAccountFailure")
    }

    fun logCreateAccountSuccess() {
        log("action" to "createAccountSuccess")
    }

    fun logError(code: String?) {
        log("action" to "error", "error_text" to code)
    }

    fun logSuccess() {
        log("action" to "success")
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppLogin"
        private const val REVISION = 20710032
        const val SOURCE_NAV = "navigation"
        const val SOURCE_EDIT = "edit"
        const val SOURCE_BLOCKED = "blocked"
        const val SOURCE_SYSTEM = "system"
        const val SOURCE_ONBOARDING = "onboarding"
        const val SOURCE_SETTINGS = "settings"
        const val SOURCE_READING_MANUAL_SYNC = "reading_lists_manual_sync"
        const val SOURCE_LOGOUT_BACKGROUND = "logout_background"
        const val SOURCE_SUGGESTED_EDITS = "suggestededits"
    }
}
