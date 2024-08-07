package org.wikipedia.gallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

class ImagePipelineBitmapGetter(context: Context, imageUrl: String?, transform: BitmapTransformation? = null, callback: Callback) {
    fun interface Callback {
        fun onSuccess(bitmap: Bitmap)
    }

    init {
        Glide.with(context)
            .asBitmap()
            .let { if (transform != null) it.transform(transform) else it }
            .load(imageUrl)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    callback.onSuccess(resource)
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }
}
