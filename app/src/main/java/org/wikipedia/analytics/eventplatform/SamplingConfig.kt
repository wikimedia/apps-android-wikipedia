package org.wikipedia.analytics.eventplatform

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents the sampling config component of a stream configuration.
 *
 * The boxed Double type is used instead of the double primitive because its value may be null,
 * which denotes that the stream should always be *included*.
 */
@JsonClass(generateAdapter = true)
class SamplingConfig @JvmOverloads constructor(val rate: Double = 1.0, val identifier: Identifier = Identifier.SESSION) {
    enum class Identifier {
        @Json(name = "pageview") PAGEVIEW,
        @Json(name = "session") SESSION,
        @Json(name = "device") DEVICE
    }
}
