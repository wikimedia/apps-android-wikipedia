package org.wikipedia.page.linkpreview

import android.location.Location
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.extensions.parcelable
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L
import java.util.Date

class LinkPreviewViewModel(bundle: Bundle) : ViewModel() {
    private val _uiState = MutableStateFlow<LinkPreviewViewState>(LinkPreviewViewState.Loading)
    val uiState = _uiState.asStateFlow()
    val historyEntry = bundle.parcelable<HistoryEntry>(LinkPreviewDialog.ARG_ENTRY)!!
    var pageTitle = historyEntry.title
    val location = bundle.parcelable<Location>(LinkPreviewDialog.ARG_LOCATION)
    val fromPlaces = bundle.getBoolean(LinkPreviewDialog.ARG_FROM_PLACES, false)
    var isWatched = false

    init {
        loadContent()
    }

    private fun loadContent() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _uiState.value = LinkPreviewViewState.Error(throwable)
        }) {
            val response = ServiceFactory.getRest(pageTitle.wikiSite)
                .getSummaryResponseSuspend(pageTitle.prefixedText, null, null, null, null, null)

            val summary = response.body()!!
            // Rebuild our PageTitle, since it may have been redirected or normalized.
            val oldFragment = pageTitle.fragment
            pageTitle = PageTitle(
                    summary.apiTitle, pageTitle.wikiSite, summary.thumbnailUrl,
                    summary.description, summary.displayTitle
            )

            // check if our URL was redirected, which might include a URL fragment that leads
            // to a specific section in the target article.
            if (!response.raw().request.url.fragment.isNullOrEmpty()) {
                pageTitle.fragment = response.raw().request.url.fragment
            } else if (!oldFragment.isNullOrEmpty()) {
                pageTitle.fragment = oldFragment
            }

            _uiState.value = LinkPreviewViewState.Content(summary)
        }
    }

    fun loadWatchStatus() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _uiState.value = LinkPreviewViewState.Error(throwable)
        }) {
            val watchStatus = ServiceFactory.get(pageTitle.wikiSite).getWatchedStatus(pageTitle.prefixedText).query?.firstPage()
            isWatched = watchStatus?.watched ?: false
            _uiState.value = LinkPreviewViewState.Watch(isWatched, Date().time)
        }
    }

    fun loadGallery(revision: Long) {
        if (Prefs.isImageDownloadEnabled) {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                L.w("Failed to fetch gallery collection.", throwable)
            }) {
                val mediaList = ServiceFactory.getRest(pageTitle.wikiSite)
                    .getMediaListSuspend(pageTitle.prefixedText, revision)
                val maxImages = 10
                val items = mediaList.getItems("image", "video").asReversed()
                val titleList =
                    items.filter { it.showInGallery }.map { it.title }.take(maxImages)
                if (titleList.isEmpty()) _uiState.value = LinkPreviewViewState.Completed
                else {
                    val response = ServiceFactory.get(
                        pageTitle.wikiSite
                    ).getImageInfoSuspend(
                        titleList.joinToString("|"),
                        pageTitle.wikiSite.languageCode
                    )
                    val pageList =
                        response.query?.pages?.filter { it.imageInfo() != null }.orEmpty()
                    _uiState.value = LinkPreviewViewState.Gallery(pageList)
                }
            }
        } else {
            _uiState.value = LinkPreviewViewState.Completed
        }
    }

    class Factory(private val bunble: Bundle) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LinkPreviewViewModel(bunble) as T
        }
    }
}
