package org.wikipedia.dataclient.mwapi

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.analytics.eventplatform.StreamConfig

@JsonClass(generateAdapter = true)
class MwStreamConfigsResponse(
    errors: List<MwServiceError> = emptyList(),
    @Json(name = "servedby") servedBy: String = "",
    @Json(name = "streams") val streamConfigs: Map<String, StreamConfig> = emptyMap()
) : MwResponse(errors, servedBy)
