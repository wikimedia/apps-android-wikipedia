package org.wikipedia.descriptions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import org.wikipedia.Constants
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.PageSummaryForEdit

class DescriptionEditViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val pageTitle = savedStateHandle.get<PageTitle>(Constants.ARG_TITLE)!!
    val highlightText = savedStateHandle.get<String>(Constants.ARG_HIGHLIGHT_TEXT)
    val action = savedStateHandle.get<DescriptionEditActivity.Action>(Constants.INTENT_EXTRA_ACTION)!!
    val invokeSource = savedStateHandle.get<Constants.InvokeSource>(Constants.INTENT_EXTRA_INVOKE_SOURCE)!!
    val sourceSummary = savedStateHandle.get<PageSummaryForEdit>(Constants.ARG_SOURCE_SUMMARY)
    val targetSummary = savedStateHandle.get<PageSummaryForEdit>(Constants.ARG_TARGET_SUMMARY)
}
