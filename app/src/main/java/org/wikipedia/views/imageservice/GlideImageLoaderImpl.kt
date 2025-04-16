package org.wikipedia.views.imageservice

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import org.wikipedia.settings.Prefs
import org.wikipedia.util.CenterCropWithFaceTransformation
import org.wikipedia.util.DimenUtil.roundedDpToPx
import org.wikipedia.util.WhiteBackgroundTransformation
import org.wikipedia.views.ViewUtil.getPlaceholderDrawable

class GlideImageLoaderImpl : ImageLoaderImpl {
    private val CENTER_CROP_ROUNDED_CORNERS = MultiTransformation(
        CenterCrop(), WhiteBackgroundTransformation(), RoundedCorners(
            roundedDpToPx(2f)
        )
    )
    override fun loadImage(
        imageView: ImageView,
        url: String?,
        roundedCorners: Boolean,
        force: Boolean,
        placeholderId: Int?,
        listener: ImageLoadListener?
    ) {
        val imageUrl = if ((Prefs.isImageDownloadEnabled || force) && !url.isNullOrEmpty()) Uri.parse(url) else null
        var builder = Glide.with(imageView)
            .load(imageUrl)
            .downsample(DownsampleStrategy.CENTER_INSIDE)

        if (placeholderId != null) {
            builder = builder.placeholder(placeholderId).error(placeholderId)
        } else {
            val placeholder = getPlaceholderDrawable(imageView.context)
            builder = builder.placeholder(placeholder).error(placeholder)
        }

        builder = if (roundedCorners) {
            builder.transform(CENTER_CROP_ROUNDED_CORNERS)
        } else {
            builder.transform(WhiteBackgroundTransformation())
        }

        if (listener != null) {
            builder = builder.listener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    listener.onError(e ?: Exception("Unknown error loading image"))
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable?>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    listener.onSuccess(imageView)
                    return false
                }
            })
        }
        builder.into(imageView)
    }

    override fun loadImage(
        imageView: ImageView,
        uri: Uri?,
        shouldDetectFace: Boolean,
        cropped: Boolean,
        emptyPlaceholder: Boolean,
        listener: ImageLoadListener?
    ) {
        val placeholder = getPlaceholderDrawable(imageView.context)
        if (!Prefs.isImageDownloadEnabled || uri == null) {
            imageView.setImageDrawable(placeholder)
            return
        }
        var builder = Glide.with(imageView)
            .load(uri)
            .placeholder(if (emptyPlaceholder) null else placeholder)
            .error(placeholder)
            .downsample(DownsampleStrategy.CENTER_INSIDE)
        if (listener != null) {
            builder = builder.listener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    listener.onError(e ?: Exception("Unknown error loading image"))
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable?>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    if (resource is BitmapDrawable && resource.bitmap != null) {
                        listener.onSuccess(Palette.from(resource.bitmap).generate(), resource.bitmap.width, resource.bitmap.height)
                    } else {
                        listener.onError(Exception("Unknown error loading image"))
                    }
                    return false
                }
            })
        }
        builder = if (cropped) {
            if (shouldDetectFace) {
                builder.transform(FACE_DETECT_TRANSFORM)
            } else {
                builder.transform(CENTER_CROP_WHITE_BACKGROUND)
            }
        } else {
            builder.transform(WhiteBackgroundTransformation())
        }
        builder.into(imageView)
    }

    override fun imagePipeLineBitmapGetter(
        context: Context,
        imageUrl: String?,
        imageTransformer: ImageTransformer?,
        onSuccess: (Bitmap) -> Unit
    ) {
        Glide.with(context)
            .asBitmap()
            .let {
                if (imageTransformer != null && imageTransformer is WhiteBackgroundTransformation)
                    it.transform(imageTransformer) else it
            }
            .load(imageUrl)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    onSuccess(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    override fun getBitmapForMarker(context: Context): Bitmap {
        val markerSize = roundedDpToPx(40f)
        return Glide.get(context).bitmapPool
            .getDirty(markerSize, markerSize, Bitmap.Config.ARGB_8888)
    }

    override fun getWhiteBackgroundTransformer(): ImageTransformer {
        return WhiteBackgroundTransformation()
    }

    companion object {
        private val FACE_DETECT_TRANSFORM by lazy { CenterCropWithFaceTransformation() }
        private val CENTER_CROP_WHITE_BACKGROUND by lazy { MultiTransformation(CenterCrop(), WhiteBackgroundTransformation()) }
    }
}
