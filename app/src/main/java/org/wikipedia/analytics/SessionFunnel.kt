package org.wikipedia.analytics

import android.text.format.DateUtils
import org.wikipedia.WikipediaApp
import org.wikipedia.history.HistoryEntry
import org.wikipedia.settings.Prefs
import org.wikipedia.util.StringUtil

class SessionFunnel(app: WikipediaApp) : Funnel(app, SCHEMA_NAME, REVISION) {

    private var sessionData: SessionData
    private var leadSectionStartTime: Long = 0

    init {
        sessionData = Prefs.getSessionData()
        if (sessionData.startTime == 0L || sessionData.lastTouchTime == 0L) {
            // session was serialized/deserialized incorrectly, so reset it.
            sessionData = SessionData()
            persistSession()
        }
        touchSession()
    }

    /**
     * Save the state of the current session. To be called when the main Activity is stopped,
     * so that we don't have to save its state every time a single parameter is modified.
     */
    fun persistSession() {
        Prefs.setSessionData(sessionData)
    }

    /**
     * Update the timestamp for the current session. If the last-updated time is older than the
     * defined timeout period, then consider the current session as over, and send the event!
     */
    fun touchSession() {
        val now = System.currentTimeMillis()
        if (hasTimedOut()) {
            logSessionData()
            // start a new session by clearing everything.
            sessionData = SessionData()
            persistSession()
        }
        sessionData.lastTouchTime = now
    }

    fun pageViewed(entry: HistoryEntry?) {
        touchSession()
        sessionData.addPageViewed(entry!!)
    }

    fun backPressed() {
        touchSession()
        sessionData.addPageFromBack()
    }

    fun noDescription() {
        touchSession()
        sessionData.addPageWithNoDescription()
    }

    fun leadSectionFetchStart() {
        leadSectionStartTime = System.currentTimeMillis()
    }

    fun leadSectionFetchEnd() {
        sessionData.addLeadLatency(System.currentTimeMillis() - leadSectionStartTime)
    }

    private fun hasTimedOut(): Boolean {
        return System.currentTimeMillis() - sessionData.lastTouchTime > Prefs.getSessionTimeout() * DateUtils.MINUTE_IN_MILLIS
    }

    private fun logSessionData() {
        val sessionLengthSeconds = (sessionData.lastTouchTime - sessionData.startTime) / DateUtils.SECOND_IN_MILLIS
        log(
                "length", sessionLengthSeconds,
                "fromSearch", sessionData.pagesFromSearch,
                "fromRandom", sessionData.pagesFromRandom,
                "fromLanglink", sessionData.pagesFromLangLink,
                "fromInternal", sessionData.pagesFromInternal,
                "fromExternal", sessionData.pagesFromExternal,
                "fromHistory", sessionData.pagesFromHistory,
                "fromReadingList", sessionData.pagesFromReadingList,
                "fromBack", sessionData.pagesFromBack,
                "noDescription", sessionData.pagesWithNoDescription,
                "fromSuggestedEdits", sessionData.pagesFromSuggestedEdits,
                "totalPages", sessionData.totalPages,
                "pageLoadLatency", sessionData.getLeadLatency(),
                "languages", StringUtil.listToJsonArrayString(app.language().appLanguageCodes),
                "apiMode", 1
        )
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppSessions"
        private const val REVISION = 19851683

        /**
         * Definition of a "session timeout", as agreed upon by the Apps and Analytics teams.
         * (currently 30 minutes)
         */
        const val DEFAULT_SESSION_TIMEOUT = 30
        const val MIN_SESSION_TIMEOUT = 1
    }
}
