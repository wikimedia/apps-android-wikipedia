package org.wikipedia.analytics.metricsplatform

import org.wikipedia.WikipediaApp

object ApplicationData {

    val data = mapOf(
        "app_theme" to WikipediaApp.instance.currentTheme,
        "language_groups" to WikipediaApp.instance.languageState.appLanguageCodes
    )

    fun getApplicationData(): Map<String, Any> {
        return data
    }
}
