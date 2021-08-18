package org.wikipedia.gallery

class MediaList {

    private val revision: String? = null
    private val tid: String? = null
    private val items: List<MediaListItem>? = null

    fun getItems(vararg types: String): MutableList<MediaListItem> {
        val list = mutableListOf<MediaListItem>()
        items?.let { mediaList ->
            list.addAll(mediaList.filter { it.showInGallery && types.contains(it.type) })
        }
        return list
    }
}
