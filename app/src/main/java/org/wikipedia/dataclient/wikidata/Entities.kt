package org.wikipedia.dataclient.wikidata

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import org.wikipedia.dataclient.mwapi.MwResponse

@Serializable
class Entities : MwResponse() {

    var entities: Map<String, Entity> = emptyMap()
        private set
    val first: Entity?
        get() = entities.values.firstOrNull()

    init {
        entities = entities.filter { it.key != "-1" && it.value.missing == null }
    }

    @Serializable
    class Entity {

        val id: String = ""
        val labels: Map<String, Label> = emptyMap()
        val descriptions: Map<String, Label> = emptyMap()
        val sitelinks: Map<String, SiteLink> = emptyMap()
        val missing: JsonElement? = null
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
