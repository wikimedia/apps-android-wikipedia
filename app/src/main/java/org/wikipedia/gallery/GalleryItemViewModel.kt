package org.wikipedia.gallery

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.extensions.parcelable
import org.wikipedia.page.PageTitle
import org.wikipedia.util.FileUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.StringUtil

class GalleryItemViewModel(bundle: Bundle) : ViewModel() {

    var mediaListItem = bundle.parcelable<MediaListItem>(GalleryItemFragment.ARG_GALLERY_ITEM)!!
    val pageTitle = bundle.parcelable<PageTitle>(Constants.ARG_TITLE) ?: PageTitle(mediaListItem.title, Constants.commonsWikiSite)
    var imageTitle = PageTitle("File:${StringUtil.removeNamespace(mediaListItem.title)}", pageTitle.wikiSite)
    var mediaPage: MwQueryPage? = null

    private val _uiState = MutableStateFlow(Resource<Boolean>())
    val uiState = _uiState.asStateFlow()

    init {
        loadMedia()
    }

    fun loadMedia() {
        _uiState.value = Resource.Loading()
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _uiState.value = Resource.Error(throwable)
        }) {
            val wikiSite = if (mediaListItem.isInCommons) Constants.commonsWikiSite else pageTitle.wikiSite
            val response = if (mediaListItem.isVideo) {
                ServiceFactory.get(wikiSite).getVideoInfo(pageTitle.prefixedText, WikipediaApp.instance.appOrSystemLanguageCode)
            } else {
                ServiceFactory.get(wikiSite).getImageInfoSuspend(pageTitle.prefixedText, WikipediaApp.instance.appOrSystemLanguageCode)
            }
            mediaPage = response.query?.firstPage()
            _uiState.value = Resource.Success(FileUtil.isVideo(mediaPage?.imageInfo()?.mime.orEmpty()))
        }
    }

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GalleryItemViewModel(bundle) as T
        }
    }
}
