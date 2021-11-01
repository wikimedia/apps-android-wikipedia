package org.wikipedia.dataclient.wikidata

import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.mwapi.MwPostResponse

@Serializable
class EntityPostResponse : MwPostResponse() {
    val entity: Entities.Entity? = null
}
