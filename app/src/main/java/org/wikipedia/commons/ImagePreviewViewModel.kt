package org.wikipedia.commons

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.extensions.parcelable
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.util.Resource

class ImagePreviewViewModel(bundle: Bundle) : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = Resource.Error(throwable)
    }
    var pageSummaryForEdit = bundle.parcelable<PageSummaryForEdit>(ImagePreviewDialog.ARG_SUMMARY)!!
    var action = bundle.getSerializable(ImagePreviewDialog.ARG_ACTION) as DescriptionEditActivity.Action?

    private val _uiState = MutableStateFlow(Resource<FilePage>())
    val uiState = _uiState.asStateFlow()

    init {
        loadImageInfo()
    }

    private fun loadImageInfo() {
        _uiState.value = Resource.Loading()
        viewModelScope.launch(handler) {
            var isFromCommons = false
            var firstPage = ServiceFactory.get(Constants.commonsWikiSite)
                .getImageInfoSuspend(pageSummaryForEdit.title, pageSummaryForEdit.lang).query?.firstPage()

            if (firstPage?.imageInfo() == null) {
                // If file page originally comes from *.wikipedia.org (i.e. movie posters), it will not have imageInfo and pageId.
                firstPage = ServiceFactory.get(pageSummaryForEdit.pageTitle.wikiSite)
                    .getImageInfoSuspend(pageSummaryForEdit.title, pageSummaryForEdit.lang).query?.firstPage()
            } else {
                // Fetch API from commons.wikimedia.org and check whether if it is not a "shared" image.
                isFromCommons = !(firstPage.isImageShared)
            }

            firstPage?.imageInfo()?.let { imageInfo ->
                pageSummaryForEdit.timestamp = imageInfo.timestamp
                pageSummaryForEdit.user = imageInfo.user
                pageSummaryForEdit.metadata = imageInfo.metadata

                val imageTagsResponse = async { ImageTagsProvider.getImageTags(firstPage.pageId, pageSummaryForEdit.lang) }

                val filePage = FilePage().apply {
                    imageFromCommons = isFromCommons
                    page = firstPage
                    imageTags = imageTagsResponse.await()
                    thumbnailWidth = imageInfo.thumbWidth
                    thumbnailHeight = imageInfo.thumbHeight
                }

                _uiState.value = Resource.Success(filePage)
            } ?: run {
                _uiState.value = Resource.Error(Throwable("No image info found."))
            }
        }
    }

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ImagePreviewViewModel(bundle) as T
        }
    }
}
