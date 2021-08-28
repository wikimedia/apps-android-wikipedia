package org.wikipedia

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import org.wikipedia.analytics.eventplatform.EventPlatformClient
import org.wikipedia.main.MainActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.Theme

class ActivityLifecycleHandler : ActivityLifecycleCallbacks, ComponentCallbacks2 {

    private var haveMainActivity = false
    var isAnyActivityResumed = false

    fun haveMainActivity(): Boolean {
        return haveMainActivity
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        val app = WikipediaApp.instance
        if (activity is MainActivity) {
            haveMainActivity = true
        }
        if (Prefs.shouldMatchSystemTheme() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val currentTheme = app.currentTheme
            when (app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> if (!app.currentTheme.isDark) {
                    app.currentTheme = if (!app.unmarshalTheme(Prefs.getPreviousThemeId()).isDark) Theme.BLACK
                    else app.unmarshalTheme(Prefs.getPreviousThemeId())
                    Prefs.setPreviousThemeId(currentTheme.marshallingId)
                }
                Configuration.UI_MODE_NIGHT_NO -> if (app.currentTheme.isDark) {
                    app.currentTheme = if (app.unmarshalTheme(Prefs.getPreviousThemeId()).isDark) Theme.LIGHT
                    else app.unmarshalTheme(Prefs.getPreviousThemeId())
                    Prefs.setPreviousThemeId(currentTheme.marshallingId)
                }
            }
        }
    }

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {
        isAnyActivityResumed = true
    }

    override fun onActivityPaused(activity: Activity) {
        isAnyActivityResumed = false
    }

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (activity is MainActivity) {
            haveMainActivity = false
        }
    }

    override fun onConfigurationChanged(configuration: Configuration) {}

    override fun onLowMemory() {}

    override fun onTrimMemory(trimMemoryType: Int) {
        if (trimMemoryType == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            EventPlatformClient.flushCachedEvents()
        }
    }
}
