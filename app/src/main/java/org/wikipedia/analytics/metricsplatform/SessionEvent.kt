package org.wikipedia.analytics.metricsplatform

import android.text.format.DateUtils
import org.wikipedia.analytics.SessionData
import org.wikipedia.analytics.eventplatform.EventPlatformClient
import org.wikipedia.history.HistoryEntry
import org.wikipedia.settings.Prefs

class SessionEvent : MetricsEvent() {
    private var sessionData: SessionData
    private var pageLoadStartTime: Long = 0

    init {
        sessionData = Prefs.sessionData
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
        Prefs.sessionData = sessionData
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
            EventPlatformClient.AssociationController.beginNewSession()
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

    fun pageFetchStart() {
        pageLoadStartTime = System.nanoTime()
    }

    fun pageFetchEnd() {
        sessionData.addPageLatency(System.nanoTime() - pageLoadStartTime)
    }

    private fun hasTimedOut(): Boolean {
        return System.currentTimeMillis() - sessionData.lastTouchTime > Prefs.sessionTimeout * DateUtils.MINUTE_IN_MILLIS
    }

    private fun logSessionData() {
        val sessionLength = (sessionData.lastTouchTime - sessionData.startTime).toInt()
        submitEvent(
            "app_session",
            mapOf(
                "length_ms" to sessionLength,
                "session_data" to sessionData
            )
        )
    }

    companion object {
        /**
         * Definition of a "session timeout", as agreed upon by the Apps and Analytics teams.
         * (currently 30 minutes)
         *
         * @ToDo If/when MEP is decommissioned, replace sessionTimeout in Prefs.kt with these.
         */
        const val DEFAULT_SESSION_TIMEOUT = 30
        const val MIN_SESSION_TIMEOUT = 1
    }
}