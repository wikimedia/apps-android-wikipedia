package org.wikipedia.feed.image

import kotlinx.serialization.Serializable
import org.wikipedia.gallery.GalleryItem
import org.wikipedia.gallery.ImageInfo
import org.wikipedia.util.StringUtil

@Serializable
class FeaturedImage(
    val title: String = "",
    val image: ImageInfo = ImageInfo()
) : GalleryItem() {
    init {
        titles = Titles(title, StringUtil.addUnderscores(title), title)
        original.source = image.source
    }
}
