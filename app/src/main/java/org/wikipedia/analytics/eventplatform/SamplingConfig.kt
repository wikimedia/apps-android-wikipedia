package org.wikipedia.analytics.eventplatform

import androidx.annotation.VisibleForTesting

/**
 * Represents the sampling config component of a stream configuration.
 *
 * The boxed Double type is used instead of the double primitive because its value may be null,
 * which denotes that the stream should always be *included*.
 */
 class SamplingConfig {

    enum class Identifier {
        PAGEVIEW, SESSION, DEVICE
    }

    var rate = 1.0
        private set
    private var identifier: Identifier? = null

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
