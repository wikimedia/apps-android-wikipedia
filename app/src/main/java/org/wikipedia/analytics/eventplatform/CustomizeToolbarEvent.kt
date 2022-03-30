package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.auth.AccountUtil
import org.wikipedia.page.action.PageActionItem
import org.wikipedia.settings.Prefs

class CustomizeToolbarEvent : TimedEvent() {

    fun logCustomization(favoritesOrder: List<Int>, menuOrder: List<Int>) {
        EventPlatformClient.submit(CustomizeToolbarEventImpl(
                !AccountUtil.isLoggedIn,
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
        @SerialName("is_anon") private var isAnon: Boolean,
        @SerialName("is_rfm_enabled") private var isRfmEnabled: Boolean,
        @SerialName("favorites_order") private var favoritesOrder: List<Int>,
        @SerialName("menu_order") private var menuOrder: List<Int>,
        @SerialName("time_spent_ms") private var timeSpentMs: Int,
        private var source: String
    ) : MobileAppsEvent("android.customize_toolbar_interaction")
}
