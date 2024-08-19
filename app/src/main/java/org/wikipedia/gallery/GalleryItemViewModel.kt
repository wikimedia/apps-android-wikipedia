package org.wikipedia.gallery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.page.PageTitle
import org.wikipedia.util.FileUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.StringUtil

class GalleryItemViewModel(bundle: SavedStateHandle) : ViewModel() {
    private var mediaListItem = bundle.get<MediaListItem>(GalleryItemFragment.ARG_GALLERY_ITEM)!!
    private val pageTitle = bundle[Constants.ARG_TITLE] ?: PageTitle(mediaListItem.title, Constants.commonsWikiSite)
    var imageTitle = PageTitle("File:${StringUtil.removeNamespace(mediaListItem.title)}", pageTitle.wikiSite)
    var mediaPage: MwQueryPage? = null

    private val _uiState = MutableStateFlow(Resource<Boolean>())
    val uiState = _uiState.asStateFlow()

    init {
        loadMedia()
    }

    private fun loadMedia() {
        _uiState.value = Resource.Loading()
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _uiState.value = Resource.Error(throwable)
        }) {
            val wikiSite = if (mediaListItem.isInCommons) Constants.commonsWikiSite else imageTitle.wikiSite
            val response = if (mediaListItem.isVideo) {
                ServiceFactory.get(wikiSite).getVideoInfo(imageTitle.prefixedText, WikipediaApp.instance.appOrSystemLanguageCode)
            } else {
                ServiceFactory.get(wikiSite).getImageInfoSuspend(imageTitle.prefixedText, WikipediaApp.instance.appOrSystemLanguageCode)
            }
            mediaPage = response.query?.firstPage()
            _uiState.value = Resource.Success(FileUtil.isVideo(mediaPage?.imageInfo()?.mime.orEmpty()))
        }
    }
}
