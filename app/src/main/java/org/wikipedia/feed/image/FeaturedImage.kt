package org.wikipedia.feed.image

import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.gallery.GalleryItem
import org.wikipedia.gallery.ImageInfo
import org.wikipedia.page.PageTitle
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

    fun toPageTitle(): PageTitle {
        return PageTitle(title, WikiSite(Service.COMMONS_URL))
    }
}
