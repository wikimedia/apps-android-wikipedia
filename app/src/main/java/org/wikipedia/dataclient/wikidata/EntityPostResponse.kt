package org.wikipedia.dataclient.wikidata

import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.mwapi.MwPostResponse
import org.wikipedia.dataclient.mwapi.MwServiceError

@JsonClass(generateAdapter = true)
class EntityPostResponse(
    errors: List<MwServiceError> = emptyList(), val entity: Entities.Entity? = null
) : MwPostResponse(errors)
