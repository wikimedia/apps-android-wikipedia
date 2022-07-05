package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.Serializable

/**
 * Represents the sampling config component of a stream configuration.
 */
@Serializable
class SamplingConfig(
        val rate: Double = 1.0,
        val unit: String = UNIT_SESSION
) {
    companion object {
        const val UNIT_PAGEVIEW = "pageview"
        const val UNIT_SESSION = "session"
        const val UNIT_DEVICE = "device"
    }
}
