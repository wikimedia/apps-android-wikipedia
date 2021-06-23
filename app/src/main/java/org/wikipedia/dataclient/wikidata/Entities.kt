package org.wikipedia.dataclient.wikidata

import org.wikipedia.dataclient.mwapi.MwResponse
import org.wikipedia.json.PostProcessingTypeAdapter.PostProcessable

class Entities : MwResponse(), PostProcessable {

    val entities: Map<String, Entity>? = null
        get() = field ?: emptyMap()
    val first: Entity?
        get() = entities?.values?.iterator()?.next()

    override fun postProcess() {
        if (first != null && first?.isMissing!!) {
            throw RuntimeException("The requested entity was not found.")
        }
    }

    class Entity {

        private val id: String? = null
            get() = field.orEmpty()
        val labels: Map<String, Label>? = null
            get() = field ?: emptyMap()
        val descriptions: Map<String, Label>? = null
            get() = field ?: emptyMap()
        val sitelinks: Map<String, SiteLink>? = null
            get() = field ?: emptyMap()
        val isMissing: Boolean = false
            get() = "-1" == id && field != null
        val lastRevId: Long = 0
    }

    class Label {
        val language: String? = null
            get() = field.orEmpty()
        val value: String? = null
            get() = field.orEmpty()
    }

    class SiteLink {
        val site: String? = null
            get() = field.orEmpty()
        val title: String? = null
            get() = field.orEmpty()
    }
}
