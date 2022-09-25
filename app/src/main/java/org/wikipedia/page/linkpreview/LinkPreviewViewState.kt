package org.wikipedia.page.linkpreview

import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.page.PageSummary
import retrofit2.Response


sealed class LinkPreviewViewState {
    object Loading : LinkPreviewViewState()
    object Completed : LinkPreviewViewState()
    data class Error(val throwable: Throwable) : LinkPreviewViewState()
    data class Content(val data: Response<PageSummary>) : LinkPreviewViewState()
    data class Gallery(val data: List<MwQueryPage>) : LinkPreviewViewState()
}