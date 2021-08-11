package org.wikipedia.dataclient.watch

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.dataclient.mwapi.MwServiceError

@JsonClass(generateAdapter = true)
data class Watch(
    val errors: List<MwServiceError> = emptyList(),
    @Json(name = "servedby") val servedBy: String = "",
    val title: String = "",
    val ns: Int = 0,
    val pageid: Int = 0,
    val expiry: String = "",
    val watched: Boolean = false,
    val unwatched: Boolean = false,
    val missing: Boolean = false
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
