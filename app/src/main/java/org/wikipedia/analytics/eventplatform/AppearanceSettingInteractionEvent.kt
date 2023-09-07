package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.theme.Theme

class AppearanceSettingInteractionEvent(private val source: InvokeSource) {

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
        EventPlatformClient.submit(AppearanceSettingInteractionEventImpl(action, currentValue, newValue, source.value))
    }

    @Suppress("unused")
    @Serializable
    @SerialName("/analytics/mobile_apps/android_app_appearance_settings_interaction/1.0.0")
    class AppearanceSettingInteractionEventImpl(private val action: String,
                                                @SerialName("current_value") private val currentValue: String,
                                                @SerialName("new_value") private val newValue: String,
                                                private val source: String) :
        MobileAppsEvent("android.app_appearance_settings_interaction")
}
