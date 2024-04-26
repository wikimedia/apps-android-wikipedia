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
import org.wikipedia.extensions.parcelable
import org.wikipedia.language.LanguageUtil
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.util.Resource
import org.wikipedia.util.StringUtil

class FilePageViewModel(bundle: Bundle) : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = Resource.Error(throwable)
    }
    private val allowEdit = bundle.getBoolean(FilePageActivity.INTENT_EXTRA_ALLOW_EDIT, true)
    val pageTitle = bundle.parcelable<PageTitle>(Constants.ARG_TITLE)!!
    var pageSummaryForEdit: PageSummaryForEdit? = null

    private val _uiState = MutableStateFlow(Resource<FilePage>())
    val uiState = _uiState.asStateFlow()

    init {
        loadImageInfo()
    }

    fun loadImageInfo() {
        _uiState.value = Resource.Loading()
        viewModelScope.launch(handler) {
            var isFromCommons = false
            var firstPage = ServiceFactory.get(Constants.commonsWikiSite)
                .getImageInfoWithEntityTerms(
                    pageTitle.prefixedText, pageTitle.wikiSite.languageCode,
                    LanguageUtil.convertToUselangIfNeeded(pageTitle.wikiSite.languageCode)
                ).query?.firstPage()

            // set image caption to pageTitle description
            pageTitle.description = firstPage?.entityTerms?.label?.firstOrNull()
            if (firstPage?.imageInfo() == null) {
                // If file page originally comes from *.wikipedia.org (i.e. movie posters), it will not have imageInfo and pageId.
                firstPage = ServiceFactory.get(pageTitle.wikiSite)
                    .getImageInfoSuspend(
                        pageTitle.prefixedText,
                        pageTitle.wikiSite.languageCode
                    ).query?.firstPage()
            } else {
                // Fetch API from commons.wikimedia.org and check whether if it is not a "shared" image.
                isFromCommons = !(firstPage.isImageShared)
            }

            firstPage?.imageInfo()?.let { imageInfo ->
                pageSummaryForEdit = PageSummaryForEdit(
                    pageTitle.prefixedText,
                    pageTitle.wikiSite.languageCode,
                    pageTitle,
                    pageTitle.displayText,
                    StringUtil.fromHtml(imageInfo.metadata!!.imageDescription()).toString()
                        .ifBlank { null },
                    imageInfo.thumbUrl,
                    null,
                    null,
                    imageInfo.timestamp,
                    imageInfo.user,
                    imageInfo.metadata
                )

                val imageTagsResponse = async {
                    ImageTagsProvider.getImageTags(
                        firstPage.pageId,
                        pageSummaryForEdit!!.lang
                    )
                }
                val isEditProtected = async {
                    ServiceFactory.get(Constants.commonsWikiSite)
                        .getProtectionInfoSuspend(pageTitle.prefixedText).query?.isEditProtected
                        ?: false
                }

                val filePage = FilePage(
                    imageFromCommons = isFromCommons,
                    showEditButton = allowEdit && isFromCommons && !isEditProtected.await(),
                    showFilename = true,
                    page = firstPage,
                    imageTags = imageTagsResponse.await(),
                    thumbnailWidth = imageInfo.thumbWidth,
                    thumbnailHeight = imageInfo.thumbHeight
                )

                _uiState.value = Resource.Success(filePage)
            } ?: run {
                _uiState.value = Resource.Error(Throwable("No image info found."))
            }
        }
    }

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FilePageViewModel(bundle) as T
        }
    }
}
