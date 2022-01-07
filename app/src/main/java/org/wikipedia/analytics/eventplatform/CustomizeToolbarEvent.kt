package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.auth.AccountUtil

@Suppress("unused")
@Serializable
@SerialName("/analytics/mobile_apps/android_customize_toolbar_interaction/1.0.0")
class CustomizeToolbarEvent() : TimedEvent(STREAM_NAME) {
    private var is_anon: Boolean = false
    private var is_rfm_enabled: Boolean = false
    private var source: String = ""
    private var favorites_order: List<Int> = emptyList()
    private var menu_order: List<Int> = emptyList()
    private var time_spent_ms: Int = 0

    fun logCustomization(favoritesOrder: List<Int>, menuOrder: List<Int>) {
        this.is_anon = !AccountUtil.isLoggedIn
        this.is_rfm_enabled = false // Todo: Read from preference
        this.source = InvokeSource.PAGE_ACTIVITY.value
        this.favorites_order = favoritesOrder
        this.menu_order = menuOrder
        time_spent_ms = duration.toInt()
        EventPlatformClient.submit(this)
    }

    companion object {
        private const val STREAM_NAME = "android.customize_toolbar_interaction"
    }
}
