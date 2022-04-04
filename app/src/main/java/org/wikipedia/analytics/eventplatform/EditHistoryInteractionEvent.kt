package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.analytics.eventplatform.EditHistoryInteractionEvent.ActionType.*
import org.wikipedia.auth.AccountUtil

class EditHistoryInteractionEvent(private var wikiDb: String, private var pageId: Int) : TimedEvent() {

    private lateinit var action: String

    fun logShowHistory() {
        action = SHOW_HISTORY.valueString
        submitEvent()
    }

    fun logRevision() {
        action = REVISION_VIEW.valueString
        submitEvent()
    }

    fun logCompare1() {
        action = COMPARE1.valueString
        submitEvent()
    }

    fun logCompare2() {
        action = COMPARE2.valueString
        submitEvent()
    }

    fun logThankTry() {
        action = THANK_TRY.valueString
        submitEvent()
    }

    fun logThankSuccess() {
        action = THANK_SUCCESS.valueString
        submitEvent()
    }

    fun logThankFail() {
        action = THANK_FAIL.valueString
        submitEvent()
    }

    private fun submitEvent() {
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

    enum class ActionType(val valueString: String) {
        SHOW_HISTORY("show_history"),
        REVISION_VIEW("revision_view"),
        COMPARE1("compare1"),
        COMPARE2("compare2"),
        THANK_TRY("thank_try"),
        THANK_SUCCESS("thank_success"),
        THANK_FAIL("thank_fail");
    }
}
