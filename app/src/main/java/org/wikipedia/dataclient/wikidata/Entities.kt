package org.wikipedia.dataclient.wikidata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.mwapi.MwResponse
import org.wikipedia.json.PostProcessingTypeAdapter.PostProcessable

@Serializable
class Entities : MwResponse(), PostProcessable {

    val entities: Map<String, Entity> = emptyMap()
    val first: Entity?
        get() = if (entities.isEmpty()) null else entities.values.iterator().next()

    override fun postProcess() {
        if (first?.isMissing == true) {
            throw RuntimeException("The requested entity was not found.")
        }
    }

    @Serializable
    class Entity {

        private val id: String = ""
        val labels: Map<String, Label> = emptyMap()
        val descriptions: Map<String, Label> = emptyMap()
        val sitelinks: Map<String, SiteLink> = emptyMap()
        @SerialName("missing")
        val isMissing: Boolean? = null
            get() = "-1" == id && field != null
        val lastRevId: Long = 0
    }

    @Serializable
    class Label {
        val language: String = ""
        val value: String = ""
    }

    @Serializable
    class SiteLink {
        val site: String = ""
        val title: String = ""
    }
}
