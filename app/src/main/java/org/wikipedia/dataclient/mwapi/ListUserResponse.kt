package org.wikipedia.dataclient.mwapi

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class ListUserResponse(
    val name: String = "",
    @Json(name = "userid") val userId: Long = 0,
    val groups: Set<String> = emptySet(),
    val missing: Boolean = false,
    @Json(name = "cancreate") val canCreate: Boolean = false,
    @Json(name = "cancreateerror") val canCreateError: List<MwServiceError> = emptyList()
) {
    val isBlocked: Boolean
        get() = error.contains("block")
    val error: String
        get() = canCreateError.firstOrNull()?.title.orEmpty()
}
