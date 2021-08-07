package org.wikipedia.feed.image

import com.squareup.moshi.JsonClass
import org.wikipedia.gallery.GalleryItem
import org.wikipedia.gallery.ImageInfo
import org.wikipedia.util.StringUtil

@JsonClass(generateAdapter = true)
class FeaturedImage(val title: String = "", val image: ImageInfo = ImageInfo()) : GalleryItem(
    titles = Titles(title, StringUtil.addUnderscores(title), title),
    original = ImageInfo().also { it.source = image.source }
)
