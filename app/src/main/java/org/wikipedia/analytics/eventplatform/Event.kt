package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

// Base class for an Event Platform event.
// This class MUST be `sealed` for Serialization polymorphism to work automatically.
@Serializable
sealed class Event(@Transient val stream: String = "") {

    private val meta: Meta = Meta(stream)

    var dt: String? = null

    @Serializable
    private class Meta(val stream: String)
}
