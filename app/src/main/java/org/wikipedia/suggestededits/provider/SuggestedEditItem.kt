package org.wikipedia.suggestededits.provider

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.wikidata.Entities
import org.wikipedia.gallery.GalleryItem

@Serializable
class SuggestedEditItem {
    private val pageid: Int = 0
    private val ns: Int = 0
    private val title: String? = null
    @SerialName("structured")
    private val structuredData: GalleryItem.StructuredData? = null
    @SerialName("wikibase_item")
    val entity: Entities.Entity? = null

    fun title(): String {
        return title.orEmpty()
    }

    val captions get() = structuredData?.captions ?: emptyMap()
}
