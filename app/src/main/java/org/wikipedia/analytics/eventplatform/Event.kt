package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.wikipedia.util.DateUtil
import java.util.*

// Base class for an Event Platform event.
// This class MUST be `sealed` for Serialization polymorphism to work automatically.
@Serializable
sealed class Event(@Transient val stream: String = "") {

    private val meta = Meta(stream)
    private val dt: String

    init {
        // Note: DO NOT join the declaration of these fields with the assignment. This seems to be
        // necessary for polymorphic serialization.
        dt = DateUtil.iso8601DateFormat(Date())
    }

    @Serializable
    private class Meta(val stream: String)
}
