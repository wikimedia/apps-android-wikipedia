package org.wikipedia.suggestededits.provider

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.wikidata.Entities
import org.wikipedia.gallery.GalleryItem

@JsonClass(generateAdapter = true)
class SuggestedEditItem(
    internal val pageid: Int = 0,
    internal val ns: Int = 0,
    internal val title: String = "",
    @Json(name = "structured")
    internal val structuredData: GalleryItem.StructuredData? = null,
    @Json(name = "wikibase_item")
    val entity: Entities.Entity? = null,
) {
    val captions get() = structuredData?.captions ?: emptyMap()
}
