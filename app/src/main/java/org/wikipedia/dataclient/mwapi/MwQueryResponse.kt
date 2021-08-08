package org.wikipedia.dataclient.mwapi

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
open class MwQueryResponse(
    errors: List<MwServiceError> = emptyList(),
    @Json(name = "batchcomplete") val batchComplete: Boolean = false,
    @Json(name = "continue") val continuation: Map<String, String> = emptyMap(),
    var query: MwQueryResult? = null
) : MwResponse(errors)
