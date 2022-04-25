package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.auth.AccountUtil
import org.wikipedia.theme.Theme

class AppearanceSettingInteractionEvent(private var source: InvokeSource) {

    fun logFontSizeChange(currentFontSize: Float, newFontSize: Float) {
        submitEvent("fontSizeChange", currentFontSize.toString(), newFontSize.toString())
    }

    fun logThemeChange(currentTheme: Theme, newTheme: Theme) {
        submitEvent("themeChange", currentTheme.funnelName, newTheme.funnelName)
    }

    fun logFontThemeChange(currentFontFamily: String?, newFontFamily: String?) {
        submitEvent("fontThemeChange", currentFontFamily.orEmpty(), newFontFamily.orEmpty())
    }

    fun logReadingFocusMode(newValue: Boolean) {
        submitEvent("readingFocusMode", (!newValue).toString(), newValue.toString())
    }

    private fun submitEvent(action: String, currentValue: String, newValue: String) {
        EventPlatformClient.submit(AppearanceSettingInteractionEventImpl(action, !AccountUtil.isLoggedIn, currentValue, newValue, source.value))
    }

    @Suppress("unused")
    @Serializable
    @SerialName("/analytics/mobile_apps/android_app_appearance_settings_interaction/1.0.0")
    class AppearanceSettingInteractionEventImpl(private var action: String,
                                                @SerialName("is_anon") private val isAnon: Boolean,
                                                @SerialName("current_value") private var currentValue: String,
                                                @SerialName("new_value") private var newValue: String,
                                                private var source: String) :
        MobileAppsEvent("android.app_appearance_settings_interaction")
}
