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
            errors.firstOrNull { it.key.contains("blocked") || it.key.startsWith("abusefilter-") }?.let {
                throw MwException(it)
            }
            errors.firstOrNull { it.key.contains("assertuserfailed") || it.key.contains("notloggedin") }?.let {
                throw MwNotLoggedInException(it)
            }
            throw MwException(errors.first())
        }
    }
}
