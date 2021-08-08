package org.wikipedia.dataclient.mwapi

import com.squareup.moshi.Json

abstract class MwResponse(
    val errors: List<MwServiceError> = emptyList(),
    @Json(name = "servedby") internal val servedBy: String? = null
) {
    init {
        if (errors.isNotEmpty()) {
            for (error in errors) {
                // prioritize "blocked" errors over others.
                if (error.title.contains("blocked")) {
                    throw MwException(error)
                }
            }
            throw MwException(errors[0])
        }
    }
}
