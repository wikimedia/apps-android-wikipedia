package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.auth.AccountUtil
import org.wikipedia.page.action.PageActionItem
import org.wikipedia.settings.Prefs

@Suppress("unused")
@Serializable
@SerialName("/analytics/mobile_apps/android_customize_toolbar_interaction/1.0.0")
class CustomizeToolbarEvent : TimedEvent(STREAM_NAME) {
    private var is_anon: Boolean? = null
    private var is_rfm_enabled: Boolean? = null
    private var source: String = ""
    private var favorites_order: List<Int> = emptyList()
    private var menu_order: List<Int> = emptyList()
    private var time_spent_ms: Int = 0

    fun logCustomization(favoritesOrder: List<Int>, menuOrder: List<Int>) {
        is_anon = !AccountUtil.isLoggedIn
        is_rfm_enabled = Prefs.readingFocusModeEnabled
        this.source = if (Prefs.customizeToolbarMenuOrder.contains(PageActionItem.THEME.id))
            InvokeSource.PAGE_OVERFLOW_MENU.value else InvokeSource.PAGE_ACTION_TAB.value
        this.favorites_order = favoritesOrder
        this.menu_order = menuOrder
        time_spent_ms = duration.toInt()
        EventPlatformClient.submit(this)
    }

    companion object {
        private const val STREAM_NAME = "android.customize_toolbar_interaction"
    }
}
