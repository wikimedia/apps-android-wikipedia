package org.wikipedia.suggestededits.provider

import com.google.gson.annotations.SerializedName
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.gallery.GalleryItem
import org.wikipedia.wikidata.Entities

class SuggestedEditItem : PageSummary() {
    @SerializedName("structured") private val structuredData: GalleryItem.StructuredData? = null
    @SerializedName("wikibase_item") val entity: Entities.Entity? = null

    val captions: Map<String, String>
        get() = if (structuredData != null && structuredData.captions != null) structuredData.captions as Map<String, String> else emptyMap()
}
