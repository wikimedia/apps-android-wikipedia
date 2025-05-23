package org.wikipedia.views.imageservice

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.annotation.DrawableRes

object ImageService {
    private var _implementation: ImageServiceLoader? = null
    private val implementation get() = _implementation ?: throw IllegalStateException(
        "ImageService has not been set, call ImageService.setImplementation() before using ImageService"
    )

    fun setImplementation(impl: ImageServiceLoader) {
        _implementation = impl
    }

    fun loadImage(
        imageView: ImageView,
        url: String?,
        detectFace: Boolean? = false,
        force: Boolean? = false,
        @DrawableRes placeholderId: Int? = null,
        listener: ImageLoadListener? = null
    ) {
        implementation.loadImage(imageView, url, detectFace, force, placeholderId, listener)
    }

    fun loadImage(
        context: Context,
        url: String?,
        whiteBackground: Boolean = false,
        onSuccess: (Bitmap) -> Unit) {
        implementation.loadImage(context, url, whiteBackground, onSuccess)
    }

    fun getBitmap(image: Any): Bitmap {
        return implementation.getBitmap(image)
    }
}

interface ImageLoadListener {
    fun onSuccess(image: Any, width: Int, height: Int) {}
    fun onError(error: Throwable) {}
}

interface ImageServiceLoader {
    fun loadImage(
        imageView: ImageView,
        url: String?,
        detectFace: Boolean? = false,
        force: Boolean? = false,
        @DrawableRes placeholderId: Int? = null,
        listener: ImageLoadListener? = null
    )

    fun loadImage(
        context: Context,
        imageUrl: String?,
        whiteBackground: Boolean = false,
        onSuccess: (Bitmap) -> Unit
    )

    fun getBitmap(image: Any): Bitmap
}
