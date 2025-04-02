package org.wikipedia.views.imageservice

import android.net.Uri
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

    override fun loadImage(
        imageView: ImageView,
        uri: Uri?,
        shouldDetectFace: Boolean,
        cropped: Boolean,
        emptyPlaceholder: Boolean,
        listener: ImageLoadListener?
    ) {}
}
