package org.wikipedia.feed.image

import org.wikipedia.gallery.GalleryItem
import org.wikipedia.gallery.ImageInfo
import org.wikipedia.json.PostProcessingTypeAdapter.PostProcessable
import org.wikipedia.json.annotations.Required
import org.wikipedia.util.StringUtil.addUnderscores

class FeaturedImage : GalleryItem(), PostProcessable {

    @Required
    val title: String? = null

    @Required
    val image: ImageInfo? = null

    override fun postProcess() {
        titles = Titles(title!!, addUnderscores(title), title)
        original.setSource(image!!.source)
    }
}
