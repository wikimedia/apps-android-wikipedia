package org.wikipedia.views.imageservice

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.imageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import coil3.request.target
import coil3.request.transformations
import coil3.toBitmap
import coil3.transform.RoundedCornersTransformation
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil
import org.wikipedia.views.ViewUtil

class CoilLoaderImpl : ImageLoaderImpl {
    override fun loadImage(
        imageView: ImageView,
        url: String?,
        roundedCorners: Boolean,
        force: Boolean,
        placeholderId: Int?,
        listener: ImageLoadListener?
    ) {
        val context = imageView.context
        val imageUrl = if ((Prefs.isImageDownloadEnabled || force) && !url.isNullOrEmpty()) url.toUri() else null
        val requestBuilder = ImageRequest.Builder(imageView.context)
            .data(imageUrl)
            .crossfade(true)

        if (placeholderId != null) {
            requestBuilder
                .placeholder(placeholderId)
                .error(placeholderId)
        } else {
            val placeHolder = ViewUtil.getPlaceholderDrawable(context)
            requestBuilder.placeholder(placeHolder)
                .error(placeHolder)
        }

        if (roundedCorners) {
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            requestBuilder.transformations(
                RoundedCornersTransformation(2f),
                CoilWhiteBackgroundTransformation()
            )
        } else {
            requestBuilder.transformations(CoilWhiteBackgroundTransformation())
        }

        if (listener != null) {
            requestBuilder.listener(object : ImageRequest.Listener {
                override fun onError(request: ImageRequest, result: ErrorResult) {
                    listener.onError(error = result.throwable)
                }

                override fun onSuccess(request: ImageRequest, result: SuccessResult) {
                    listener.onSuccess(imageView)
                }
            })
        }
        val request = requestBuilder
            .target(imageView).build()
        context.imageLoader.enqueue(request)
    }

    override fun loadImage(
        imageView: ImageView,
        uri: Uri?,
        shouldDetectFace: Boolean,
        cropped: Boolean,
        emptyPlaceholder: Boolean,
        listener: ImageLoadListener?
    ) {
        val context = imageView.context
        val placeholder = ViewUtil.getPlaceholderDrawable(context)
        if (!Prefs.isImageDownloadEnabled || uri == null) {
            imageView.setImageDrawable(placeholder)
            return
        }

        val requestBuilder = ImageRequest.Builder(context)
            .data(uri)
            .placeholder(if (emptyPlaceholder) null else placeholder)
            .error(placeholder)

        if (listener != null) {
            requestBuilder.listener(object : ImageRequest.Listener {
                override fun onError(request: ImageRequest, result: ErrorResult) {
                    listener.onError(result.throwable)
                }

                override fun onSuccess(request: ImageRequest, result: SuccessResult) {
                    val bitmapImage = result.image.toBitmap()
                    listener.onSuccess(
                        Palette.from(bitmapImage).generate(),
                        bitmapImage.width,
                        bitmapImage.height
                    )
                }
            })
        }
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        requestBuilder
            .transformations(CoilWhiteBackgroundTransformation())
        val request = requestBuilder.target(imageView).build()
        context.imageLoader.enqueue(request)
    }

    override fun imagePipeLineBitmapGetter(
        context: Context,
        imageUrl: String?,
        imageTransformer: ImageTransformer?,
        onSuccess: (Bitmap) -> Unit
    ) {
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .apply {
                if (imageTransformer != null && imageTransformer is CoilWhiteBackgroundTransformation) {
                    transformations(imageTransformer)
                }
            }
            .target(
                onSuccess = { result ->
                    onSuccess(result.toBitmap())
                }
            )
            .build()
        ImageLoader(context).enqueue(request)
    }

    override fun getBitmapForMarker(context: Context): Bitmap {
        val markerSize = DimenUtil.roundedDpToPx(40f)
        return createBitmap(markerSize, markerSize, Bitmap.Config.ARGB_8888)
    }

    override fun getWhiteBackgroundTransformer(): ImageTransformer {
        return CoilWhiteBackgroundTransformation()
    }
}
