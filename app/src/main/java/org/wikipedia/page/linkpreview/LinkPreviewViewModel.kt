package org.wikipedia.page.linkpreview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.select.Collector.collect
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.gallery.MediaList
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L

class LinkPreviewViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<LinkPreviewViewState>(LinkPreviewViewState.Loading)
    val uiState = _uiState.asStateFlow()

    fun loadContent(pageTitle: PageTitle) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _uiState.value = LinkPreviewViewState.Error(throwable)
        }) {
            withContext(Dispatchers.IO)
            {
                val response = ServiceFactory.getRest(pageTitle.wikiSite)
                    .getSummaryResponseSuspend(pageTitle.prefixedText, null, null, null, null, null)
                _uiState.value = LinkPreviewViewState.Content(response)

            }
        }
    }

    fun loadGallery(pageTitle: PageTitle, revision: Long) {
        if (Prefs.isImageDownloadEnabled) {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                L.w("Failed to fetch gallery collection.", throwable)
            }) {
                withContext(Dispatchers.IO)
                {
                   val mediaList =  ServiceFactory.getRest(pageTitle.wikiSite)
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
            }
        } else {
            _uiState.value = LinkPreviewViewState.Completed
        }
    }
}