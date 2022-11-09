package org.wikipedia.gallery

import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.wikipedia.commons.ImageTagsProvider
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle

class GalleryViewModel(bundle: Bundle) : ViewModel() {

    var pageTitle = bundle.parcelable<PageTitle>(GalleryActivity.EXTRA_PAGETITLE)
    var fileName = bundle.getString(GalleryActivity.EXTRA_FILENAME)
    var revision = bundle.getLong(GalleryActivity.EXTRA_REVISION)
    var source = bundle.getInt(GalleryActivity.EXTRA_SOURCE)
    var wiki = bundle.parcelable<WikiSite>(GalleryActivity.EXTRA_WIKI)

    private val repository = GalleryRepository()

    private val _mediaListItem =
        MutableLiveData<GalleryViewState<MutableList<MediaListItem>>>(GalleryViewState.InitialState)
    val mediaListItem: LiveData<GalleryViewState<MutableList<MediaListItem>>> = _mediaListItem

    private val _imageCaption =
        MutableLiveData<GalleryViewState<Triple<Map<String, String>, Boolean, Int>>>(
            GalleryViewState.InitialState
        )
    val imageCaption: LiveData<GalleryViewState<Triple<Map<String, String>, Boolean, Int>>> =
        _imageCaption

    fun fetchGalleryItems(pageTitle: PageTitle, revision: Long) {
        viewModelScope.launch {
            _mediaListItem.postValue(GalleryViewState.Loading)
            repository.fetchGalleryItems(pageTitle, revision)
                .flowOn(Dispatchers.IO)
                .catch { e -> _mediaListItem.postValue(GalleryViewState.Failed(e)) }
                .collect { response ->
                    val mediaListItem = response.getItems("image", "video")
                    _mediaListItem.postValue(GalleryViewState.Success(mediaListItem))
                }
        }
    }

    fun fetchImageCaption(text: String) {
        viewModelScope.launch {
            _imageCaption.postValue(GalleryViewState.Loading)

            val entitiesFlow = repository.fetchEntitiesByTitle(text)
            val mwQueryResponseFlow = repository.fetchProtectionInfo(text)

            entitiesFlow.zip(mwQueryResponseFlow) { en, res ->
                val captionsMap = en.first?.labels?.values?.associate { it.language to it.value }.orEmpty()
                val tagsCount = ImageTagsProvider.getDepictsClaims(en.first?.getStatements().orEmpty()).size
                val isProtected = res.query?.isEditProtected
                return@zip Triple(captionsMap, isProtected == true, tagsCount)
            }
                .flowOn(Dispatchers.IO)
                .catch { e -> _imageCaption.postValue(GalleryViewState.Failed(e)) }
                .collect { _imageCaption.postValue(GalleryViewState.Success(it)) }
        }
    }

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GalleryViewModel(bundle) as T
        }
    }
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}
