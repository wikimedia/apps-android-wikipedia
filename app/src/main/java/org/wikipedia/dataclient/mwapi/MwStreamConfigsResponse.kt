package org.wikipedia.dataclient.mwapi

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.analytics.eventplatform.StreamConfig

@JsonClass(generateAdapter = true)
class MwStreamConfigsResponse(@Json(name = "streams") val streamConfigs: Map<String, StreamConfig>) : MwResponse()
