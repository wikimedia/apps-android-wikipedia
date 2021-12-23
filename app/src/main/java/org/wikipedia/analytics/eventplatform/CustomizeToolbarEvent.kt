package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.auth.AccountUtil

@Suppress("unused")
@Serializable
@SerialName("/analytics/mobile_apps/android_customize_toolbar_interaction/1.0.0")
class CustomizeToolbarEvent(private var is_anon: Boolean,
                            private var is_rfm_enabled: Boolean,
                            private var source: String,
                            private var favorites_order: List<Int>,
                            private var menu_order: List<Int>,
                            private var time_spent_ms: Int) : TimedEvent(STREAM_NAME) {

    fun logCustomization(source: String, favorites_order: List<Int>, menu_order: List<Int>, is_rfm_enabled: Boolean) {
        this.is_anon = AccountUtil.isLoggedIn
        this.is_rfm_enabled = is_rfm_enabled
        this.source = source
        this.favorites_order = favorites_order
        this.menu_order = menu_order
        time_spent_ms = duration.toInt()
        EventPlatformClient.submit(this)
    }

    companion object {
        private const val STREAM_NAME = "android.customize_toolbar_interaction"
    }
}
