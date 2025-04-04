package org.wikipedia.views.imageservice

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.widget.ImageView
import androidx.palette.graphics.Palette

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

    fun loadImage(
        imageView: ImageView,
        url: Uri?,
        shouldDetectFace: Boolean = true,
        cropped: Boolean = true,
        emptyPlaceholder: Boolean = false,
        listener: ImageLoadListener? = null
    ) {
        implementation.loadImage(imageView, url, shouldDetectFace, cropped, emptyPlaceholder, listener)
    }
}

interface ImageLoadListener {
    fun onSuccess(view: ImageView) {}
    fun onSuccess(palette: Palette, bmpWidth: Int, bmpHeight: Int) {}
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

    fun loadImage(
        imageView: ImageView,
        uri: Uri?,
        shouldDetectFace: Boolean = true,
        cropped: Boolean = true,
        emptyPlaceholder: Boolean = false,
        listener: ImageLoadListener? = null
    ) {}
}

interface ImageTransformation {
    fun apply(bitmap: Bitmap, width: Int, height: Int): Bitmap
    fun dimImage(bitmap: Bitmap): Bitmap
    fun applyMatrixWithBackground(inBitmap: Bitmap, targetBitmap: Bitmap, matrix: Matrix)
}

interface ImageLibraryConfig {
    fun initialize(context: Context)
}
