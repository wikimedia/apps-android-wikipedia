package org.wikimedia.metrics_platform.config.sampling

import kotlinx.serialization.Serializable

@Serializable
class SampleConfig(
    val rate: Double = 1.0,
    val unit: String = UNIT_SESSION
) {
    companion object {
        const val UNIT_PAGEVIEW = "pageview"
        const val UNIT_SESSION = "session"
        const val UNIT_DEVICE = "device"
    }
}
