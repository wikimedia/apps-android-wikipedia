package org.wikipedia.gallery

class MediaList {

    private val revision: String? = null
    private val tid: String? = null
    private val items: List<MediaListItem>? = null

    fun getItems(vararg types: String): List<MediaListItem> {
        val list = mutableListOf<MediaListItem>()
        items?.filter { it.showInGallery() }?.forEach { mediaListItem ->
            types.filter { type -> mediaListItem.type.contains(type) }
                .forEach { _ -> list.add(mediaListItem) }
        }

        return list
    }
}