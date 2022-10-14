package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.page.action.PageActionItem
import org.wikipedia.settings.Prefs

class CustomizeToolbarEvent : TimedEvent() {

    fun logCustomization(favoritesOrder: List<Int>, menuOrder: List<Int>) {
        EventPlatformClient.submit(CustomizeToolbarEventImpl(
                Prefs.readingFocusModeEnabled,
                favoritesOrder,
                menuOrder,
                duration,
                if (Prefs.customizeToolbarMenuOrder.contains(PageActionItem.THEME.id))
                    InvokeSource.PAGE_OVERFLOW_MENU.value else InvokeSource.PAGE_ACTION_TAB.value)
        )
    }

    @Suppress("unused")
    @Serializable
    @SerialName("/analytics/mobile_apps/android_customize_toolbar_interaction/1.0.0")
    class CustomizeToolbarEventImpl(
        @SerialName("is_rfm_enabled") private val isRfmEnabled: Boolean,
        @SerialName("favorites_order") private val favoritesOrder: List<Int>,
        @SerialName("menu_order") private val menuOrder: List<Int>,
        @SerialName("time_spent_ms") private val timeSpentMs: Int,
        private val source: String
    ) : MobileAppsEvent("android.customize_toolbar_interaction")
}
