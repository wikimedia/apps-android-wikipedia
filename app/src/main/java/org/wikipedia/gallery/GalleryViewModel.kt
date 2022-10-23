package org.wikipedia.gallery

import android.os.Bundle
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle

class GalleryViewModel(bundle: Bundle) : ViewModel() {

    var pageTitle = bundle.getParcelable<PageTitle>(GalleryActivity.EXTRA_PAGETITLE)
    var fileName = bundle.getString(GalleryActivity.EXTRA_FILENAME)
    var revision = bundle.getLong(GalleryActivity.EXTRA_REVISION)
    var source = bundle.getInt(GalleryActivity.EXTRA_SOURCE)
    var wiki = bundle.getParcelable<WikiSite>(GalleryActivity.EXTRA_WIKI)

    private val repository = GalleryRepository()

    private val _mediaListItem =
        MutableLiveData<GalleryViewState<MutableList<MediaListItem>>>(GalleryViewState.InitialState)
    val mediaListItem: LiveData<GalleryViewState<MutableList<MediaListItem>>> = _mediaListItem

    fun fetchGalleryItems(pageTitle: PageTitle, revision: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _mediaListItem.postValue(GalleryViewState.Loading)
            try {
                repository.fetchGalleryItems(pageTitle, revision).collect { response ->
                    if (response.isSuccessful) {
                        _mediaListItem.postValue(GalleryViewState.Loading)
                        val mediaListItem = response.body()?.getItems("image", "video")
                        _mediaListItem.postValue(GalleryViewState.Success(mediaListItem))
                    }
                }
            } catch (e: Exception) {
                _mediaListItem.postValue(GalleryViewState.Failed(e))
            }
        }
    }

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GalleryViewModel(bundle) as T
        }
    }
}
