package org.wikipedia.suggestededits.provider

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.wikidata.Entities
import org.wikipedia.gallery.GalleryItem

@Serializable
class SuggestedEditItem {
    private val pageid: Int = 0
    private val ns: Int = 0
    private val title: String? = null
    @SerializedName("structured")
    private val structuredData: GalleryItem.StructuredData? = null
    @SerializedName("wikibase_item")
    val entity: Entities.Entity? = null

    fun title(): String {
        return title.orEmpty()
    }

    val captions: Map<String, String>
        get() = if (structuredData != null && structuredData.captions != null) structuredData.captions as Map<String, String> else emptyMap()
}
