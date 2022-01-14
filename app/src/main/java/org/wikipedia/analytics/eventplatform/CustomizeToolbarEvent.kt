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
    @SerialName("is_anon") private var isAnon: Boolean? = null
    @SerialName("is_rfm_enabled") private var isRfmEnabled: Boolean? = null
    @SerialName("favorites_order") private var favoritesOrder: List<Int> = emptyList()
    @SerialName("menu_order") private var menuOrder: List<Int> = emptyList()
    @SerialName("time_spent_ms") private var timeSpentMs: Int = 0
    private var source: String = ""

    fun logCustomization(favoritesOrder: List<Int>, menuOrder: List<Int>) {
        this.isAnon = !AccountUtil.isLoggedIn
        this.isRfmEnabled = Prefs.readingFocusModeEnabled
        this.source = if (Prefs.customizeFavoritesMenuOrder.contains(PageActionItem.THEME.id))
            InvokeSource.PAGE_OVERFLOW_MENU.value else InvokeSource.PAGE_ACTION_TAB.value
        this.favoritesOrder = favoritesOrder
        this.menuOrder = menuOrder
        this.timeSpentMs = duration.toInt()
        EventPlatformClient.submit(this)
    }

    companion object {
        private const val STREAM_NAME = "android.customize_toolbar_interaction"
    }
}
