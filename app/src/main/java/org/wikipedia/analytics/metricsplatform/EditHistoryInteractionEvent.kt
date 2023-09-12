package org.wikipedia.analytics.metricsplatform

import org.wikimedia.metrics_platform.context.PageData
import org.wikipedia.diff.ArticleEditDetailsViewModel
import org.wikipedia.page.PageTitle
import org.wikipedia.page.edithistory.EditHistoryListViewModel

class EditHistoryInteractionEvent : TimedMetricsEvent {
    private val pageData: PageData?

    constructor(viewModel: ArticleEditDetailsViewModel) {
        this.pageData = getPageData(viewModel, viewModel.revisionFromId)
    }

    constructor(viewModel: EditHistoryListViewModel) {
        this.pageData = getPageData(viewModel)
    }

    constructor(pageTitle: PageTitle, pageId: Int, revisionId: Long) {
        this.pageData = getPageData(pageTitle, pageId, revisionId)
    }

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
        submitEvent(
            "edit_history_interaction",
            mapOf(
                "action" to action,
                "time_spent_ms" to timer.elapsedMillis
            ),
            pageData
        )
    }
}
