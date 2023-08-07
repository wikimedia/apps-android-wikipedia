package org.wikipedia.analytics.metricsplatform

import org.wikipedia.Constants
import org.wikipedia.page.action.PageActionItem
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.Theme

class AppearanceSettingInteractionEvent(private val source: Constants.InvokeSource) : MetricsEvent() {

    fun logFontSizeChange(currentFontSize: Float, newFontSize: Float) {
        submitEvent("fontSizeChange", currentFontSize.toString(), newFontSize.toString())
    }

    fun logThemeChange(currentTheme: Theme, newTheme: Theme) {
        submitEvent("themeChange", currentTheme.tag, newTheme.tag)
    }

    fun logFontThemeChange(currentFontFamily: String?, newFontFamily: String?) {
        submitEvent("fontThemeChange", currentFontFamily.orEmpty(), newFontFamily.orEmpty())
    }

    fun logReadingFocusMode(newValue: Boolean) {
        submitEvent("readingFocusMode", (!newValue).toString(), newValue.toString())
    }

    private fun submitEvent(action: String, currentValue: String, newValue: String) {
        submitEvent(
            "app_appearance_settings_interaction",
            mapOf(
                "action" to action,
                "current_value" to currentValue,
                "new_value" to newValue,
                "source" to source.value
            )
        )
    }
}

class CustomizeToolbarEvent : TimedMetricsEvent() {

    fun logCustomization(favoritesOrder: List<Int>, menuOrder: List<Int>) {
        val source = if (Prefs.customizeToolbarMenuOrder.contains(PageActionItem.THEME.id))
            Constants.InvokeSource.PAGE_OVERFLOW_MENU.value else Constants.InvokeSource.PAGE_ACTION_TAB.value

        submitEvent(
            "customize_toolbar_interaction",
            mapOf(
                "is_rfm_enabled" to Prefs.readingFocusModeEnabled,
                "favorites_order" to favoritesOrder,
                "menu_order" to menuOrder,
                "time_spent_ms" to timer.elapsedMillis,
                "source" to source
            )
        )
    }
}
