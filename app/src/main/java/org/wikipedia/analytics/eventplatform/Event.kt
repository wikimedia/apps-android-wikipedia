package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Instant
import java.util.*

// Base class for an Event Platform event.
// This class MUST be `sealed` for Serialization polymorphism to work automatically.
@Suppress("unused")
@Serializable
sealed class Event(@Transient val stream: String = "") {
    private val meta = Meta(stream)
    @Required private val dt = Instant.now().toString()

    @Serializable
    private class Meta(@Required val stream: String)
}
