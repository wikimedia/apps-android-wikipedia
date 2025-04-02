package org.wikipedia.views

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import com.google.android.material.imageview.ShapeableImageView
import org.wikipedia.views.imageservice.ImageLoadListener
import org.wikipedia.views.imageservice.ImageService

class FaceAndColorDetectImageView : ShapeableImageView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    private fun shouldDetectFace(uri: Uri?): Boolean {
        if (uri == null) return false
        // TODO: not perfect; should ideally detect based on MIME type.
        val path = uri.path.orEmpty()
        return path.endsWith(".jpg", true) || path.endsWith(".jpeg", true)
    }

    fun loadImage(uri: Uri?, cropped: Boolean = true, emptyPlaceholder: Boolean = false, listener: ImageLoadListener? = null) {
        ImageService.loadImage(this, uri, shouldDetectFace(uri), cropped, emptyPlaceholder, listener)
    }

    fun loadImage(@DrawableRes id: Int) {
        setImageResource(id)
    }
}
