package org.wikipedia.views.imageservice

import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import org.wikipedia.settings.Prefs
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
}
