package org.wikipedia.gallery

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class MediaList(
    internal val revision: String = "",
    internal val tid: String = "",
    internal val items: List<MediaListItem> = emptyList()
) {
    fun getItems(vararg types: String): MutableList<MediaListItem> {
        return items.filter { it.isShowInGallery && types.contains(it.type) }.toMutableList()
    }
}
