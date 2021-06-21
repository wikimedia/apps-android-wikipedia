package org.wikipedia.gallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

abstract class ImagePipelineBitmapGetter(private val imageUrl: String?) {

    abstract fun onSuccess(bitmap: Bitmap?)

    operator fun get(context: Context) {
        Glide.with(context)
            .asBitmap()
            .load(imageUrl)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    onSuccess(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }
}
