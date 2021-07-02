package org.wikipedia.feed.image

import org.wikipedia.gallery.GalleryItem
import org.wikipedia.gallery.ImageInfo
import org.wikipedia.json.PostProcessingTypeAdapter.PostProcessable
import org.wikipedia.util.StringUtil

class FeaturedImage : GalleryItem(), PostProcessable {

    val title = ""

    val image = ImageInfo()

    override fun postProcess() {
        titles = Titles(title, StringUtil.addUnderscores(title), title)
        original.setSource(image.source)
    }
}
