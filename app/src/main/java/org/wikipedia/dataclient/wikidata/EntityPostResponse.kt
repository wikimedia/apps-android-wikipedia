package org.wikipedia.dataclient.wikidata

import org.wikipedia.dataclient.mwapi.MwPostResponse

class EntityPostResponse : MwPostResponse() {
    val entity: Entities.Entity? = null
}