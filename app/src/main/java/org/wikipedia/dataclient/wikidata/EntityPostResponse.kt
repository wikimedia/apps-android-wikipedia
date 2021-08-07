package org.wikipedia.dataclient.wikidata

import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.mwapi.MwPostResponse

@JsonClass(generateAdapter = true)
class EntityPostResponse(val entity: Entities.Entity? = null) : MwPostResponse()
