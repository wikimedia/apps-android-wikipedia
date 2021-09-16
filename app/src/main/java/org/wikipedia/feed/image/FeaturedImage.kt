package org.wikipedia.feed.image

import kotlinx.serialization.Serializable
import org.wikipedia.gallery.GalleryItem
import org.wikipedia.gallery.ImageInfo
import org.wikipedia.util.StringUtil

@Serializable
class FeaturedImage : GalleryItem() {

    val title = ""

    val image = ImageInfo()

    init {
        titles = Titles(title, StringUtil.addUnderscores(title), title)
        original.source = image.source
    }
}
