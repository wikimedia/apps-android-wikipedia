package org.wikipedia.views

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import org.wikipedia.settings.Prefs
import org.wikipedia.util.CenterCropWithFaceTransformation
import org.wikipedia.util.WhiteBackgroundTransformation

class FaceAndColorDetectImageView : AppCompatImageView {

    interface OnImageLoadListener {
        fun onImageLoaded(palette: Palette, bmpWidth: Int, bmpHeight: Int)
        fun onImageFailed()
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    private fun shouldDetectFace(uri: Uri): Boolean {
        // TODO: not perfect; should ideally detect based on MIME type.
        val path = uri.path.orEmpty()
        return path.endsWith(".jpg", true) || path.endsWith(".jpeg", true)
    }

    fun loadImage(uri: Uri?, roundedCorners: Boolean = false, cropped: Boolean = true, emptyPlaceholder: Boolean = false, listener: OnImageLoadListener? = null) {
        val placeholder = ViewUtil.getPlaceholderDrawable(context)
        if (!Prefs.isImageDownloadEnabled || uri == null) {
            setImageDrawable(placeholder)
            return
        }
        var builder = Glide.with(this)
                .load(uri)
                .placeholder(if (emptyPlaceholder) null else placeholder)
                .error(placeholder)
                .downsample(DownsampleStrategy.CENTER_INSIDE)
        if (listener != null) {
            builder = builder.listener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable?>, isFirstResource: Boolean): Boolean {
                    listener.onImageFailed()
                    return false
                }

                override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable?>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                    if (resource is BitmapDrawable && resource.bitmap != null) {
                        listener.onImageLoaded(Palette.from(resource.bitmap).generate(), resource.bitmap.width, resource.bitmap.height)
                    } else {
                        listener.onImageFailed()
                    }
                    return false
                }
            })
        }
        builder = if (cropped) {
            if (shouldDetectFace(uri)) {
                builder.transform(if (roundedCorners) FACE_DETECT_TRANSFORM_AND_ROUNDED_CORNERS else FACE_DETECT_TRANSFORM)
            } else {
                builder.transform(if (roundedCorners) ViewUtil.CENTER_CROP_LARGE_ROUNDED_CORNERS else CENTER_CROP_WHITE_BACKGROUND)
            }
        } else {
            builder.transform(WhiteBackgroundTransformation())
        }
        builder.into(this)
    }

    fun loadImage(@DrawableRes id: Int) {
        setImageResource(id)
    }

    companion object {
        private val FACE_DETECT_TRANSFORM by lazy { CenterCropWithFaceTransformation() }
        private val FACE_DETECT_TRANSFORM_AND_ROUNDED_CORNERS by lazy { MultiTransformation(FACE_DETECT_TRANSFORM, ViewUtil.ROUNDED_CORNERS) }
        private val CENTER_CROP_WHITE_BACKGROUND by lazy { MultiTransformation(CenterCrop(), WhiteBackgroundTransformation()) }
    }
}
