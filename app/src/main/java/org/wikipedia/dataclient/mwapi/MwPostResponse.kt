package org.wikipedia.dataclient.mwapi

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
open class MwPostResponse(
    errors: List<MwServiceError> = emptyList(),
    @Json(name = "servedby") servedBy: String = "",
    val pageInfo: MwQueryPage? = null,
    val options: String = "",
    val success: Int = 0
) : MwResponse(errors, servedBy)
