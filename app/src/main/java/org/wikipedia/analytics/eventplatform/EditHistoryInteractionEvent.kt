package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.auth.AccountUtil

class EditHistoryInteractionEvent(private var wikiDb: String, private var pageId: Int) :
    TimedEvent() {

    fun logShowHistory() {
        submitEvent("show_history")
    }

    fun logRevision() {
        submitEvent("revision_view")
    }

    // User tapped 'Compare' on the edit History screen to start selecting the revisions to compare
    fun logCompare1() {
        submitEvent("compare1")
    }

    // User has selected a second revision and tapped the 'Compare' button, navigating them to the comparison screen
    fun logCompare2() {
        submitEvent("compare2")
    }

    fun logThankTry() {
        submitEvent("thank_try")
    }

    fun logThankSuccess() {
        submitEvent("thank_success")
    }

    fun logThankFail() {
        submitEvent("thank_fail")
    }

    private fun submitEvent(action: String) {
        EventPlatformClient.submit(EditHistoryInteractionEventImpl(!AccountUtil.isLoggedIn, duration, wikiDb, pageId, action))
    }

    @Suppress("unused")
    @Serializable
    @SerialName("/analytics/mobile_apps/android_edit_history_interaction/1.0.0")
    class EditHistoryInteractionEventImpl(@SerialName("is_anon") private val isAnon: Boolean,
                                      @SerialName("time_spent_ms") private var timeSpentMs: Int,
                                      @SerialName("wiki_db") private var wikiDb: String,
                                      @SerialName("page_id") private var pageId: Int,
                                      private val action: String) :
        MobileAppsEvent("android.edit_history_interaction")
}
