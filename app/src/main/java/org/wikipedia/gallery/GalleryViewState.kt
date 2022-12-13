package org.wikipedia.gallery

sealed class GalleryViewState<out T> {
    object InitialState : GalleryViewState<Nothing>()
    object Loading : GalleryViewState<Nothing>()
    data class Success<out T>(val data: T? = null) : GalleryViewState<T>()
    data class Failed(val throwable: Throwable?) : GalleryViewState<Nothing>()
}
