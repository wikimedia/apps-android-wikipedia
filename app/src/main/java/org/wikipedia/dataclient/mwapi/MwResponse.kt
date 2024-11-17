package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
abstract class MwResponse {
    private val errors: List<MwServiceError>? = null

    @SerialName("servedby")
    private val servedBy: String? = null

    init {
        if (errors?.isNotEmpty() == true) {
            // prioritize "blocked" or "abusefilter" errors over others.
            throw MwException(errors.firstOrNull { it.key.contains("blocked") || it.key.startsWith("abusefilter-") } ?: errors.first())
        }
    }
}
