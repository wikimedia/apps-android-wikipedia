package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Instant

// Base class for an Event Platform event.
// This class MUST be `sealed` for Serialization polymorphism to work automatically.
@Suppress("unused")
@Serializable
sealed class Event(@Transient val stream: String = "") {
    private val meta = Meta(stream)

    @Serializable
    private class Meta(
        val stream: String,
        val dt: String = Instant.now().toString()
    )
}
