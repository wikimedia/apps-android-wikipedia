package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.theme.Theme

class AppearanceChangeFunnel(app: WikipediaApp, wiki: WikiSite, private val source: InvokeSource) :
        Funnel(app, "MobileWikiAppAppearanceSettings", 22451226, wiki) {

    fun logFontSizeChange(currentFontSize: Float, newFontSize: Float) {
        log(
            "action" to "fontSizeChange",
            "current_value" to currentFontSize.toString(),
            "new_value" to newFontSize.toString()
        )
    }

    fun logThemeChange(currentTheme: Theme, newTheme: Theme) {
        log(
            "action" to "themeChange",
            "current_value" to currentTheme.funnelName,
            "new_value" to newTheme.funnelName
        )
    }

    fun logFontThemeChange(currentFontFamily: String?, newFontFamily: String?) {
        log(
            "action" to "fontThemeChange",
            "current_value" to currentFontFamily,
            "new_value" to newFontFamily
        )
    }

    fun logReadingFocusMode(newValue: Boolean) {
        log(
            "action" to "readingFocusMode",
            "current_value" to (!newValue).toString(),
            "new_value" to newValue.toString()
        )
    }

    override fun preprocessSessionToken(eventData: JSONObject) {}

    override fun preprocessData(eventData: JSONObject): JSONObject {
        preprocessData(eventData, "invoke_source", source.ordinal)
        preprocessData(eventData, "anon", !AccountUtil.isLoggedIn)
        return super.preprocessData(eventData)
    }
}
