package org.wikipedia.gallery

/**Created by
Author: Ankush Bose
Date: 19,May,2021
 **/

sealed class GalleryViewState<out T> {
    object InitialState : GalleryViewState<Nothing>()
    object Loading : GalleryViewState<Nothing>()
    data class Success<out T>(val data: T? = null) : GalleryViewState<T>()
    data class Failed(val throwable: Throwable?) : GalleryViewState<Nothing>()
}
