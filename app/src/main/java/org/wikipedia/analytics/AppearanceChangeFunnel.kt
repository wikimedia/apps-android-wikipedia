package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.theme.Theme

class AppearanceChangeFunnel(app: WikipediaApp, wiki: WikiSite, private val source: InvokeSource) :
        Funnel(app, SCHEMA_NAME, REV_ID, wiki) {

    fun logFontSizeChange(currentFontSize: Float, newFontSize: Float) {
        log(
                "action", "fontSizeChange",
                "current_value", currentFontSize.toString(),
                "new_value", newFontSize.toString())
    }

    fun logThemeChange(currentTheme: Theme, newTheme: Theme) {
        log(
                "action", "themeChange",
                "current_value", currentTheme.funnelName,
                "new_value", newTheme.funnelName
        )
    }

    fun logFontThemeChange(currentFontFamily: String?, newFontFamily: String?) {
        log(
                "action", "fontThemeChange",
                "current_value", currentFontFamily,
                "new_value", newFontFamily
        )
    }

    override fun preprocessSessionToken(eventData: JSONObject) {}

    override fun preprocessData(eventData: JSONObject): JSONObject {
        preprocessData(eventData, "invoke_source", source.ordinal)
        return super.preprocessData(eventData)
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppAppearanceSettings"
        private const val REV_ID = 20566858
    }
}
