package org.wikipedia.dataclient.mwapi

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
open class MwPostResponse(
    errors: List<MwServiceError> = emptyList(), val pageInfo: MwQueryPage? = null,
    val options: String? = null, val success: Int = 0
) : MwResponse(errors)
