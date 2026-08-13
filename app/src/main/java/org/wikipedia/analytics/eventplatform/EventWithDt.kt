package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Instant

@Suppress("unused")
@Serializable
sealed class EventWithDt(@Transient private val _streamName: String = "") : Event(_streamName) {
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val dt: String = Instant.now().toString()
}
