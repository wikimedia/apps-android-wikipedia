package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("unused")
@Serializable
@SerialName("/analytics/mobile_apps/android_customize_toolbar_interaction/1.0.0")
class CustomizeToolbarEvent(private val is_anon: Boolean,
                            private val source: String,
                            private val favorites_order: List<Int>,
                            private val menu_order: List<Int>,
                            private var time_spent_ms: Int) : TimedEvent(STREAM_NAME) {

    fun logCustomization(source: String, favorites_order: List<Int>, menu_order: List<Int>) {
        time_spent_ms = duration.toInt()
        EventPlatformClient.submit(this)
    }

    companion object {
        private const val STREAM_NAME = "android.customize_toolbar_interaction"
    }
}
