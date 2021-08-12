package org.wikipedia.feed.image

import kotlinx.serialization.Serializable
import org.wikipedia.gallery.GalleryItem
import org.wikipedia.gallery.ImageInfo
import org.wikipedia.json.PostProcessingTypeAdapter.PostProcessable
import org.wikipedia.util.StringUtil

@Serializable
class FeaturedImage : GalleryItem(), PostProcessable {

    val title = ""

    val image = ImageInfo()

    override fun postProcess() {
        titles = Titles(title, StringUtil.addUnderscores(title), title)
        original.setSource(image.source)
    }
}
