package org.wikipedia.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Build
import android.os.Handler
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import kotlin.system.exitProcess

object DeviceUtil {
    private inline val Window.insetsControllerCompat
        get() = WindowCompat.getInsetsController(this, decorView)

    fun showSoftKeyboard(view: View) {
        (view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    fun hideSoftKeyboard(activity: Activity) {
        activity.window.insetsControllerCompat.hide(WindowInsetsCompat.Type.ime())
    }

    fun hideSoftKeyboard(view: View) {
        ViewCompat.getWindowInsetsController(view)?.hide(WindowInsetsCompat.Type.ime())
    }

    fun isHardKeyboardAttached(resources: Resources): Boolean {
        return (resources.configuration.hardKeyboardHidden == Configuration.KEYBOARDHIDDEN_NO &&
                resources.configuration.keyboard != Configuration.KEYBOARD_UNDEFINED &&
                resources.configuration.keyboard != Configuration.KEYBOARD_NOKEYS)
    }

    fun setLightSystemUiVisibility(activity: Activity) {
        // this make the system recognizes the status bar light and will make status bar icons become visible
        // if the theme is not dark
        activity.window.insetsControllerCompat.isAppearanceLightStatusBars = !WikipediaApp.instance.currentTheme.isDark
    }

    fun setNavigationBarColor(window: Window, @ColorInt color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val isDarkThemeOrDarkBackground = WikipediaApp.instance.currentTheme.isDark ||
                    color == ContextCompat.getColor(window.context, android.R.color.black)
            window.navigationBarColor = color
            window.insetsControllerCompat.isAppearanceLightNavigationBars = !isDarkThemeOrDarkBackground
        }
    }

    fun updateStatusBarTheme(activity: Activity, toolbar: MaterialToolbar?, reset: Boolean) {
        activity.window.insetsControllerCompat.isAppearanceLightStatusBars = !reset ||
                !WikipediaApp.instance.currentTheme.isDark
        toolbar?.setNavigationIconTint(if (reset) Color.WHITE else ResourceUtil.getThemedColor(activity, R.attr.primary_color))
    }

    fun setContextClickAsLongClick(vararg views: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            views.forEach {
                it.setOnContextClickListener { obj -> obj.performLongClick() }
            }
        }
    }

    /**
     * https://issuetracker.google.com/issues/160946170
     * There's a platform-specific issue where the app gets launched in "Restricted" mode during a
     * backup operation; And if the app crashes during that operation, it remains in Restricted
     * mode in subsequent launches, including subsequent user-requested launches. While in this
     * mode, the system doesn't actually launch our custom subclassed WikipediaApp object, but
     * instead uses a vanilla Application object, which will cause issues when other classes try
     * to access static data from the WikipediaApp object.
     *
     * This is a workaround that explicitly terminates the app process if it's running in Restricted
     * mode, to be used sparingly from places where this crash is most likely to occur.
     */
    fun assertAppContext(context: Context, terminateOnFail: Boolean = false): Boolean {
        if (context.applicationContext !is WikipediaApp) {
            if (terminateOnFail) {
                Handler(context.mainLooper).post { exitProcess(0) }
            }
            return false
        }
        return true
    }

    fun setEdgeToEdge(activity: AppCompatActivity) {
        activity.enableEdgeToEdge()
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightStatusBars = !WikipediaApp.instance.currentTheme.isDark
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
}
