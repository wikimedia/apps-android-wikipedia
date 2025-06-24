package org.wikipedia.views.imageservice

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import coil3.Image
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.imageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowRgb565
import coil3.request.error
import coil3.request.placeholder
import coil3.request.target
import coil3.request.transformations
import coil3.toBitmap
import org.wikipedia.R
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.ResourceUtil

class CoilImageServiceLoader : ImageServiceLoader {
    private var factory = SingletonImageLoader.Factory { context ->
        ImageLoader.Builder(context)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = {
                            OkHttpConnectionFactory.client
                        }
                    )
                )
            }
            .allowRgb565(true)
            .build()
    }

    init {
        SingletonImageLoader.setSafe(factory)
    }

    override fun loadImage(
        imageView: ImageView,
        url: String?,
        detectFace: Boolean?,
        force: Boolean?,
        @DrawableRes placeholderId: Int?,
        listener: ImageLoadListener?
    ) {
        val context = imageView.context
        val request = getRequestBuilder(context, url, detectFace, force, placeholderId, listener).target(imageView).build()
        context.imageLoader.enqueue(request)
    }

    override fun loadImage(
        context: Context,
        imageUrl: String?,
        whiteBackground: Boolean,
        onSuccess: (Bitmap) -> Unit
    ) {
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .apply {
                if (whiteBackground) {
                    transformations(WhiteBackgroundTransformation())
                }
            }
            .target(
                onSuccess = { result ->
                    onSuccess(result.toBitmap())
                }
            )
            .build()
        context.imageLoader.enqueue(request)
    }

    override fun getRequest(
        context: Context,
        url: String?,
        detectFace: Boolean?,
        force: Boolean?,
        placeholderId: Int?,
        listener: ImageLoadListener?
    ): Any {
        return getRequestBuilder(context, url, detectFace, force, placeholderId, listener).build()
    }

    fun getRequestBuilder(
        context: Context,
        url: String?,
        detectFace: Boolean?,
        force: Boolean?,
        placeholderId: Int?,
        listener: ImageLoadListener?
    ): ImageRequest.Builder {
        val imageUrl = if ((Prefs.isImageDownloadEnabled || force == true) && !url.isNullOrEmpty()) url.toUri() else null
        val requestBuilder = ImageRequest.Builder(context)
            .data(imageUrl)

        if (placeholderId != null) {
            requestBuilder.placeholder(placeholderId).error(placeholderId)
        } else {
            val placeHolder = ResourceUtil.getThemedColor(context, R.attr.border_color).toDrawable()
            requestBuilder.placeholder(placeHolder).error(placeHolder)
        }
        val isGif = ImageUrlUtil.isGif(url)
        if (!isGif) {
            when {
                (detectFace == true && shouldDetectFace(url)) -> requestBuilder.transformations(FaceDetectTransformation(), DimImageTransformation())
                else -> requestBuilder.transformations(WhiteBackgroundTransformation(), DimImageTransformation())
            }
        }

        if (listener != null) {
            requestBuilder.listener(object : ImageRequest.Listener {
                override fun onError(request: ImageRequest, result: ErrorResult) {
                    listener.onError(error = result.throwable)
                }

                override fun onSuccess(request: ImageRequest, result: SuccessResult) {
                    listener.onSuccess(result, result.image.width, result.image.height)
                }
            })
        }
        return requestBuilder
    }

    override fun getBitmap(image: Any): Bitmap {
        if (image is Image) {
            return image.toBitmap()
        }
        if (image is Bitmap) {
            return image
        }
        return createBitmap(1, 1)
    }

    private fun shouldDetectFace(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        // TODO: not perfect; should ideally detect based on MIME type.
        return url.endsWith(".jpg", true) || url.endsWith(".jpeg", true) || url.endsWith(".png", true)
    }
}
