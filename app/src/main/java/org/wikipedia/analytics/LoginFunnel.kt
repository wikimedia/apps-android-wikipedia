package org.wikipedia.analytics

import org.wikipedia.WikipediaApp

class LoginFunnel(app: WikipediaApp) : Funnel(app, SCHEMA_NAME, REVISION) {

    @JvmOverloads
    fun logStart(source: String = "", editSessionToken: String = "") {
        log("action", "start", "source", source, "edit_session_token", editSessionToken)
    }

    fun logCreateAccountAttempt() {
        log("action", "createAccountAttempt")
    }

    fun logCreateAccountFailure() {
        log("action", "createAccountFailure")
    }

    fun logCreateAccountSuccess() {
        log("action", "createAccountSuccess")
    }

    fun logError(code: String?) {
        log("action", "error", "error_text", code)
    }

    fun logSuccess() {
        log("action", "success")
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
