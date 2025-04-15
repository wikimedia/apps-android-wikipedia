package org.wikipedia.views.imageservice

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import androidx.palette.graphics.Palette

object ImageService {
    private var implementation: ImageLoaderImpl = GlideImageLoaderImpl()

    fun setImplementation(
        context: Context,
        impl: ImageLoaderImpl) {
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

    fun imagePipeLineBitmapGetter(context: Context, imageUrl: String?, imageTransformer: ImageTransformer? = null, onSuccess: (Bitmap) -> Unit) {
        implementation.imagePipeLineBitmapGetter(context, imageUrl, imageTransformer, onSuccess)
    }

    fun getBitmapForMarker(context: Context): Bitmap {
        return implementation.getBitmapForMarker(context)
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
    )

    fun imagePipeLineBitmapGetter(
        context: Context,
        imageUrl: String?,
        imageTransformer: ImageTransformer? = null,
        onSuccess: (Bitmap) -> Unit)

    fun getBitmapForMarker(context: Context): Bitmap
}
