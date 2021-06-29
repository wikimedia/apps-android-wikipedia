package org.wikipedia.dataclient.wikidata

import com.google.gson.annotations.SerializedName
import org.wikipedia.dataclient.mwapi.MwResponse
import org.wikipedia.json.PostProcessingTypeAdapter.PostProcessable

class Entities : MwResponse(), PostProcessable {

    val entities: Map<String, Entity> = emptyMap()
    val first: Entity?
        get() = if (entities.isEmpty()) null else entities.values.iterator().next()

    override fun postProcess() {
        if (first?.isMissing == true) {
            throw RuntimeException("The requested entity was not found.")
        }
    }

    class Entity {

        private val id: String = ""
        val labels: Map<String, Label> = emptyMap()
        val descriptions: Map<String, Label> = emptyMap()
        val sitelinks: Map<String, SiteLink> = emptyMap()
        @SerializedName("missing")
        val isMissing: Boolean? = null
            get() = "-1" == id && field != null
        val lastRevId: Long = 0
    }

    class Label {
        val language: String = ""
        val value: String = ""
    }

    class SiteLink {
        val site: String = ""
        val title: String = ""
    }
}
