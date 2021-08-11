package org.wikipedia.dataclient.wikidata

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.mwapi.MwResponse
import org.wikipedia.dataclient.mwapi.MwServiceError

@JsonClass(generateAdapter = true)
class Entities(
    errors: List<MwServiceError> = emptyList(),
    @Json(name = "servedby") servedBy: String = "",
    val entities: Map<String, Entity> = emptyMap()
) : MwResponse(errors, servedBy) {
    val first: Entity?
        get() = entities.values.firstOrNull()

    init {
        if (first?.isMissing == true) {
            throw RuntimeException("The requested entity was not found.")
        }
    }

    @JsonClass(generateAdapter = true)
    class Entity(internal val id: String = "", val labels: Map<String, Label> = emptyMap(),
                 val descriptions: Map<String, Label> = emptyMap(), val sitelinks: Map<String, SiteLink> = emptyMap(),
                 val lastRevId: Long = 0) {
        @Json(name = "missing")
        val isMissing: Boolean? = null
            get() = "-1" == id && field != null
    }

    @JsonClass(generateAdapter = true)
    class Label(val language: String = "", val value: String = "")

    @JsonClass(generateAdapter = true)
    class SiteLink(val site: String = "", val title: String = "")
}
