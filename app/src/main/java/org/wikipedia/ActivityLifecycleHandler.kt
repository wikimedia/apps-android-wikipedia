package org.wikipedia

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import org.wikipedia.main.MainActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.Theme

class ActivityLifecycleHandler : ActivityLifecycleCallbacks, ComponentCallbacks2 {

    private var haveMainActivity = false
    var isAnyActivityResumed = false
    private var currentActivity: Activity? = null

    fun haveMainActivity(): Boolean {
        return haveMainActivity
    }

    fun getResumedActivity(): Activity? {
        return currentActivity
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        val app = WikipediaApp.instance
        currentActivity = activity
        if (activity is MainActivity) {
            haveMainActivity = true
        }
        if (Prefs.shouldMatchSystemTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val currentTheme = app.currentTheme
            when (app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> if (!app.currentTheme.isDark) {
                    app.currentTheme = if (!app.unmarshalTheme(Prefs.previousThemeId).isDark) Theme.BLACK
                    else app.unmarshalTheme(Prefs.previousThemeId)
                    Prefs.previousThemeId = currentTheme.marshallingId
                }
                Configuration.UI_MODE_NIGHT_NO -> if (app.currentTheme.isDark) {
                    app.currentTheme = if (app.unmarshalTheme(Prefs.previousThemeId).isDark) Theme.LIGHT
                    else app.unmarshalTheme(Prefs.previousThemeId)
                    Prefs.previousThemeId = currentTheme.marshallingId
                }
            }
        }
    }

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {
        isAnyActivityResumed = true
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        isAnyActivityResumed = false
        currentActivity = null
    }

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (activity is MainActivity) {
            haveMainActivity = false
        }
        currentActivity = null
    }

    override fun onConfigurationChanged(configuration: Configuration) {}

    override fun onLowMemory() {}

    override fun onTrimMemory(trimMemoryType: Int) {}
}
