package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.analytics.eventplatform.EditHistoryInteractionEvent.ActionType.*
import org.wikipedia.auth.AccountUtil

@Suppress("unused")
@Serializable
@SerialName("/analytics/mobile_apps/android_edit_history_interaction/1.0.0")
class EditHistoryInteractionEvent(@SerialName("wiki_db") private var wikiDb: String,
                                  @SerialName("page_id") private var pageId: Int) : TimedEvent(STREAM_NAME) {

    @SerialName("time_spent_ms") private var timeSpentMs: Int? = null
    @SerialName("is_anon") private var isAnon: Boolean? = null
    private var action: String? = null

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
        timeSpentMs = duration.toInt()
        isAnon = !AccountUtil.isLoggedIn
        EventPlatformClient.submit(this)
    }

    companion object {
        private const val STREAM_NAME = "android.edit_history_interaction"
    }

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
