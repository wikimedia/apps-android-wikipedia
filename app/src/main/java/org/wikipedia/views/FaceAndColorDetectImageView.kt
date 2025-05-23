package org.wikipedia.views

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import com.google.android.material.imageview.ShapeableImageView
import org.wikipedia.views.imageservice.ImageLoadListener
import org.wikipedia.views.imageservice.ImageService

class FaceAndColorDetectImageView : ShapeableImageView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    fun loadImage(uri: Uri?, listener: ImageLoadListener? = null) {
        ImageService.loadImage(this, uri.toString(), detectFace = true, listener = listener)
    }
}
