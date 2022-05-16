package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class EditHistoryInteractionEvent(private val wikiDb: String, private val pageId: Int) :
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

    fun logThankCancel() {
        submitEvent("thank_cancel")
    }

    fun logThankSuccess() {
        submitEvent("thank_success")
    }

    fun logThankFail() {
        submitEvent("thank_fail")
    }

    fun logSearchClick() {
        submitEvent("search_click")
    }

    fun logFilterClick() {
        submitEvent("filter_click")
    }

    fun logFilterSelection(selection: String) {
        submitEvent("filter_selection_" + selection)
    }

    fun logUndoTry() {
        submitEvent("undo_try")
    }

    fun logUndoCancel() {
        submitEvent("undo_cancel")
    }

    fun logUndoSuccess() {
        submitEvent("undo_success")
    }

    fun logUndoFail() {
        submitEvent("undo_fail")
    }

    fun logOlderEditChevronClick() {
        submitEvent("older_edit_click")
    }

    fun logNewerEditChevronClick() {
        submitEvent("newer_edit_click")
    }

    fun logShareClick() {
        submitEvent("share_click")
    }

    fun logWatchClick() {
        submitEvent("watch_click")
    }

    fun logUnwatchClick() {
        submitEvent("unwatch_click")
    }

    private fun submitEvent(action: String) {
        EventPlatformClient.submit(EditHistoryInteractionEventImpl(duration, wikiDb, pageId, action))
    }

    @Suppress("unused")
    @Serializable
    @SerialName("/analytics/mobile_apps/android_edit_history_interaction/1.0.0")
    class EditHistoryInteractionEventImpl(@SerialName("time_spent_ms") private val timeSpentMs: Int,
                                      @SerialName("wiki_db") private val wikiDb: String,
                                      @SerialName("page_id") private val pageId: Int,
                                      private val action: String) :
        MobileAppsEvent("android.edit_history_interaction")
}
