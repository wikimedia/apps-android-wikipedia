package org.wikipedia.analytics.eventplatform

import android.text.format.DateUtils
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.SessionData
import org.wikipedia.history.HistoryEntry
import org.wikipedia.settings.Prefs

class AppSessionEvent {

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
        EventPlatformClient.submit(AppSessionEventImpl(sessionLength, sessionData, WikipediaApp.instance.languageState.appLanguageCodes))
    }

    companion object {
        /**
         * Definition of a "session timeout", as agreed upon by the Apps and Analytics teams.
         * (currently 30 minutes)
         */
        const val DEFAULT_SESSION_TIMEOUT = 30
        const val MIN_SESSION_TIMEOUT = 1
    }

    @Suppress("unused")
    @Serializable
    @SerialName("/analytics/mobile_apps/app_session/1.1.0")
    class AppSessionEventImpl(@SerialName("length_ms") private val length: Int,
                              @SerialName("session_data") private val sessionData: SessionData,
                              private val languages: List<String>) :
        MobileAppsEvent("app_session")
}
