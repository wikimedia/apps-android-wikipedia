package org.wikipedia.gallery

import java.util.*

class MediaList {

    private val revision: String? = null
    private val tid: String? = null
    private val items: List<MediaListItem>? = null

    fun getItems(vararg types: String): List<MediaListItem> {
        val list: MutableList<MediaListItem> = ArrayList()
        items?.forEach {mediaListItem ->
            if (mediaListItem.showInGallery()) {
                types.filter { type -> mediaListItem.type.contains(type) }.forEach { _ -> list.add(mediaListItem) }
            }
        }

        return list
    }
}