package org.wikipedia.page.linkpreview

import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.page.PageSummary

sealed class LinkPreviewViewState {
    data object Loading : LinkPreviewViewState()
    data object Completed : LinkPreviewViewState()
    data class Error(val throwable: Throwable) : LinkPreviewViewState()
    data class Content(val data: PageSummary) : LinkPreviewViewState()
    data class Gallery(val data: List<MwQueryPage>) : LinkPreviewViewState()
    data class Watch(val isWatch: Boolean) : LinkPreviewViewState()
}
