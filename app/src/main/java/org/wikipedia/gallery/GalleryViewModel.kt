package org.wikipedia.gallery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.commons.ImageTagsProvider
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.wikidata.Entities
import org.wikipedia.page.PageTitle
import org.wikipedia.util.Resource

class GalleryViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val pageTitle = savedStateHandle.get<PageTitle>(Constants.ARG_TITLE)
    val wikiSite = savedStateHandle.get<WikiSite>(Constants.ARG_WIKISITE)!!
    val revision = savedStateHandle[GalleryActivity.EXTRA_REVISION] ?: 0L
    var initialFilename = savedStateHandle.get<String>(GalleryActivity.EXTRA_FILENAME)

    private val _uiState = MutableStateFlow(Resource<MediaList>())
    val uiState = _uiState.asStateFlow()

    private val _descriptionState = MutableStateFlow(Resource<Pair<Boolean, Entities.Entity?>>())
    val descriptionState = _descriptionState.asStateFlow()

    fun fetchGalleryItems() {
        _uiState.value = Resource.Loading()
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _uiState.value = Resource.Error(throwable)
        }) {
            pageTitle?.let {
                val response = ServiceFactory.getRest(it.wikiSite).getMediaListSuspend(it.prefixedText, revision)
                _uiState.value = Resource.Success(response)
            }
        }
    }

    fun fetchGalleryDescription(pageTitle: PageTitle) {
        _descriptionState.value = Resource.Loading()
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _descriptionState.value = Resource.Error(throwable)
        }) {
            val firstEntity = async { ServiceFactory.get(Constants.commonsWikiSite).getEntitiesByTitleSuspend(pageTitle.prefixedText, Constants.COMMONS_DB_NAME).first }
            val protectionInfoResponse = async { ServiceFactory.get(Constants.commonsWikiSite).getProtectionWithUserInfo(pageTitle.prefixedText) }
            val isProtected = protectionInfoResponse.await().query?.isEditProtected == true
            _descriptionState.value = Resource.Success(isProtected to firstEntity.await())
        }
    }

    fun getCaptions(entity: Entities.Entity?): Map<String, String> {
        return entity?.getLabels()?.values?.associate { it.language to it.value }.orEmpty()
    }

    fun getDepicts(entity: Entities.Entity?): List<String> {
        return ImageTagsProvider.getDepictsClaims(entity?.getStatements().orEmpty())
    }
}
