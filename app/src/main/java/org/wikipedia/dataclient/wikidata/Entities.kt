package org.wikipedia.dataclient.wikidata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import org.wikipedia.dataclient.mwapi.MwResponse
import org.wikipedia.json.JsonUtil

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
        val statements: JsonElement? = null
        val missing: JsonElement? = null
        @SerialName("lastrevid") val lastRevId: Long = 0

        fun getStatements(): Map<String, List<Claims.Claim>> {
            return if (statements != null && statements !is JsonArray) {
                JsonUtil.json.decodeFromJsonElement(statements)
            } else {
                emptyMap()
            }
        }
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
