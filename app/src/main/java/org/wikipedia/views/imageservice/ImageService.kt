package org.wikipedia.views.imageservice

import android.widget.ImageView

object ImageService {
    private var implementation: ImageLoaderImpl = GlideImageLoaderImpl()

    fun setImplementation(impl: ImageLoaderImpl) {
        implementation = impl
    }

    fun loadImage(
        imageView: ImageView,
        url: String?,
        roundedCorners: Boolean = false,
        force: Boolean = false,
        placeholderId: Int? = null,
        listener: ImageLoadListener? = null
    ) {
        implementation.loadImage(imageView, url, roundedCorners, force, placeholderId, listener)
    }
}

interface ImageLoadListener {
    fun onSuccess(view: ImageView)
    fun onError(error: Throwable)
}

interface ImageLoaderImpl {
    fun loadImage(
        imageView: ImageView,
        url: String?,
        roundedCorners: Boolean = false,
        force: Boolean = false,
        placeholderId: Int? = null,
        listener: ImageLoadListener? = null
    )
}
