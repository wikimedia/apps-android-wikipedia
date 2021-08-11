package org.wikipedia.dataclient.mwapi

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class MwParseResponse(
    errors: List<MwServiceError> = emptyList(),
    @Json(name = "servedby") servedBy: String = "",
    val parse: Parse = Parse()
) : MwResponse(errors, servedBy) {
    @JsonClass(generateAdapter = true)
    class Parse(val text: String = "")
}
