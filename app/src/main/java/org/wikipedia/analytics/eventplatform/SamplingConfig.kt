package org.wikipedia.analytics.eventplatform

import androidx.annotation.VisibleForTesting
import kotlinx.serialization.Serializable
import com.google.gson.annotations.SerializedName

/**
 * Represents the sampling config component of a stream configuration.
 *
 * The boxed Double type is used instead of the double primitive because its value may be null,
 * which denotes that the stream should always be *included*.
 */
@Serializable
class SamplingConfig {

    enum class Identifier {
        @SerializedName("pageview")
        PAGEVIEW,
        @SerializedName("session")
        SESSION,
        @SerializedName("device")
        DEVICE
    }

    private var identifier: Identifier? = null
    var rate = 1.0

    // This constructor is needed for correct Gson deserialization. Do not remove!
    constructor() {}

    @VisibleForTesting
    constructor(rate: Double, identifier: Identifier?) {
        this.rate = rate
        this.identifier = identifier
    }

    fun getIdentifier(): Identifier {
        return if (identifier != null) identifier!! else Identifier.SESSION
    }
}
