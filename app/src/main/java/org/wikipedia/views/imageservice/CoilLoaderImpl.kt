package org.wikipedia.views.imageservice

import android.widget.ImageView

class CoilLoaderImpl : ImageLoaderImpl {
    override fun loadImage(
        imageView: ImageView,
        url: String?,
        roundedCorners: Boolean,
        force: Boolean,
        placeholderId: Int?,
        listener: ImageLoadListener?
    ) {}
}
