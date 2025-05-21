package org.wikipedia.views.imageservice

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import androidx.palette.graphics.Palette

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
        roundedCorners: Boolean = false,
        force: Boolean = false,
        placeholderId: Int? = null,
        listener: ImageLoadListener? = null
    ) {
        implementation.loadImage(imageView, url, roundedCorners, force, placeholderId, listener)
    }

    fun loadImageWithFaceDetect(
        imageView: ImageView,
        url: Uri?,
        shouldDetectFace: Boolean = true,
        cropped: Boolean = true,
        emptyPlaceholder: Boolean = false,
        listener: ImageLoadListener? = null
    ) {
        implementation.loadImageWithFaceDetect(imageView, url, shouldDetectFace, cropped, emptyPlaceholder, listener)
    }

    fun imagePipeLineBitmapGetter(context: Context, imageUrl: String?, imageTransformer: ImageTransformer? = null, onSuccess: (Bitmap) -> Unit) {
        implementation.imagePipeLineBitmapGetter(context, imageUrl, imageTransformer, onSuccess)
    }

    fun getBitmapForMarker(context: Context): Bitmap {
        return implementation.getBitmapForMarker(context)
    }

    fun getWhiteBackgroundTransformer(): ImageTransformer {
        return implementation.getWhiteBackgroundTransformer()
    }

    fun getBitmapForWidget(
        context: Context,
        imageUrl: String?,
        onSuccess: (Bitmap) -> Unit) {
        implementation.getBitmapForWidget(context, imageUrl, onSuccess)
    }
}

interface ImageLoadListener {
    fun onSuccess(view: ImageView) {}
    fun onSuccess(palette: Palette, bmpWidth: Int, bmpHeight: Int) {}
    fun onError(error: Throwable) {}
}

interface ImageServiceLoader {
    fun loadImage(
        imageView: ImageView,
        url: String?,
        roundedCorners: Boolean = false,
        force: Boolean = false,
        placeholderId: Int? = null,
        listener: ImageLoadListener? = null
    )

    fun loadImageWithFaceDetect(
        imageView: ImageView,
        uri: Uri?,
        shouldDetectFace: Boolean = true,
        cropped: Boolean = true,
        emptyPlaceholder: Boolean = false,
        listener: ImageLoadListener? = null
    )

    fun imagePipeLineBitmapGetter(
        context: Context,
        imageUrl: String?,
        imageTransformer: ImageTransformer? = null,
        onSuccess: (Bitmap) -> Unit
    )

    fun getBitmapForWidget(
        context: Context,
        imageUrl: String?,
        onSuccess: (Bitmap) -> Unit
    )

    fun getBitmapForMarker(context: Context): Bitmap

    fun getWhiteBackgroundTransformer(): ImageTransformer
}
