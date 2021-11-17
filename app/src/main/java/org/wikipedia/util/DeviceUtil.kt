package org.wikipedia.util

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Build
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.accessibility.AccessibilityManager
import androidx.annotation.ColorInt
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import org.wikipedia.R
import org.wikipedia.WikipediaApp

object DeviceUtil {
    private inline val Window.insetsControllerCompat
        get() = WindowCompat.getInsetsController(this, decorView)

    fun showSoftKeyboard(view: View) {
        ViewCompat.getWindowInsetsController(view)?.show(WindowInsetsCompat.Type.ime())
    }

    fun hideSoftKeyboard(activity: Activity) {
        activity.window.insetsControllerCompat?.hide(WindowInsetsCompat.Type.ime())
    }

    fun hideSoftKeyboard(view: View) {
        ViewCompat.getWindowInsetsController(view)?.hide(WindowInsetsCompat.Type.ime())
    }

    fun setLightSystemUiVisibility(activity: Activity) {
        // this make the system recognizes the status bar light and will make status bar icons become visible
        // if the theme is not dark
        activity.window.insetsControllerCompat?.isAppearanceLightStatusBars = !WikipediaApp.instance.currentTheme.isDark
    }

    fun setNavigationBarColor(window: Window, @ColorInt color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val isDarkThemeOrDarkBackground = WikipediaApp.instance.currentTheme.isDark ||
                    color == ContextCompat.getColor(window.context, android.R.color.black)
            window.navigationBarColor = color
            window.insetsControllerCompat?.isAppearanceLightNavigationBars = !isDarkThemeOrDarkBackground
        }
    }

    fun updateStatusBarTheme(activity: Activity, toolbar: Toolbar?, reset: Boolean) {
        activity.window.insetsControllerCompat?.isAppearanceLightStatusBars = !reset ||
                !WikipediaApp.instance.currentTheme.isDark
        toolbar?.navigationIcon?.colorFilter = BlendModeColorFilterCompat
                .createBlendModeColorFilterCompat(if (reset) Color.WHITE
                else ResourceUtil.getThemedColor(activity, R.attr.toolbar_icon_color), BlendModeCompat.SRC_IN)
    }

    fun setContextClickAsLongClick(vararg views: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            views.forEach {
                it.setOnContextClickListener { obj -> obj.performLongClick() }
            }
        }
    }

    val isOnWiFi: Boolean
        get() {
            val info = (WikipediaApp.instance.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
                    .getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            return info != null && info.isConnected
        }

    // TODO: revisit this if there's no more navigation bar by default.
    val isNavigationBarShowing
        get() = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK) && KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME)

    val isAccessibilityEnabled: Boolean
        get() {
            val am = WikipediaApp.instance.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            // TODO: add more logic if other accessibility tools have different settings.
            return am.isEnabled && am.isTouchExplorationEnabled
        }

    val pendingIntentFlags: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
}
