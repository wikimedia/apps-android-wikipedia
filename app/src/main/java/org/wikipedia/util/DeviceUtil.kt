package org.wikipedia.util

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Build
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.wikipedia.R
import org.wikipedia.WikipediaApp

object DeviceUtil {
    private inline val Window.insetsControllerCompat: WindowInsetsControllerCompat?
        get() = WindowCompat.getInsetsController(this, decorView)

    /**
     * Attempt to display the Android keyboard.
     *
     * FIXME: This should not need to exist.
     * Android should always show the keyboard at the appropriate time. This method allows you to display the keyboard
     * when Android fails to do so.
     *
     * @param view The currently focused view that will receive the keyboard input
     */
    @JvmStatic
    fun showSoftKeyboard(view: View) {
        ViewCompat.getWindowInsetsController(view)?.show(WindowInsetsCompat.Type.ime())
    }

    /**
     * Attempt to hide the Android Keyboard.
     *
     * FIXME: This should not need to exist.
     * I do not know why Android does not handle this automatically.
     *
     * @param activity The current activity
     */
    @JvmStatic
    fun hideSoftKeyboard(activity: Activity) {
        activity.window.insetsControllerCompat?.hide(WindowInsetsCompat.Type.ime())
    }

    @JvmStatic
    fun hideSoftKeyboard(view: View) {
        ViewCompat.getWindowInsetsController(view)?.hide(WindowInsetsCompat.Type.ime())
    }

    fun setLightSystemUiVisibility(activity: Activity) {
        // this make the system recognizes the status bar light and will make status bar icons become visible
        // if the theme is not dark
        activity.window.insetsControllerCompat?.isAppearanceLightStatusBars = !WikipediaApp.getInstance().currentTheme.isDark
    }

    @JvmStatic
    fun updateStatusBarTheme(activity: Activity, toolbar: Toolbar?, reset: Boolean) {
        activity.window.insetsControllerCompat?.isAppearanceLightStatusBars = !reset ||
                !WikipediaApp.getInstance().currentTheme.isDark
        toolbar?.navigationIcon?.colorFilter = BlendModeColorFilterCompat
                .createBlendModeColorFilterCompat(if (reset) Color.WHITE
                else ResourceUtil.getThemedColor(activity, R.attr.toolbar_icon_color), BlendModeCompat.SRC_IN)
    }

    @JvmStatic
    fun setContextClickAsLongClick(vararg views: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            views.forEach {
                it.setOnContextClickListener { obj: View -> obj.performLongClick() }
            }
        }
    }

    @JvmStatic
    fun isOnWiFi(): Boolean {
        val info = (WikipediaApp.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        return info != null && info.isConnected
    }

    // TODO: revisit this if there's no more navigation bar by default.
    @JvmStatic
    val isNavigationBarShowing: Boolean
        get() = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK) && KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME)

    @JvmStatic
    val isAccessibilityEnabled: Boolean
        get() {
            val am = WikipediaApp.getInstance().getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            // TODO: add more logic if other accessibility tools have different settings.
            return am.isEnabled && am.isTouchExplorationEnabled
        }
}
