package org.wikipedia.util

import android.app.Activity
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Build
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.Toolbar
import androidx.core.content.getSystemService
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import org.wikipedia.R
import org.wikipedia.WikipediaApp

object DeviceUtil {
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
        view.context.getSystemService<InputMethodManager>()!!.toggleSoftInput(0, 0)
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
        hideSoftKeyboard(activity.window.decorView)
    }

    @JvmStatic
    fun hideSoftKeyboard(view: View) {
        // Not using getCurrentFocus as that sometimes is null, but the keyboard is still up.
        view.context.getSystemService<InputMethodManager>()!!.hideSoftInputFromWindow(view.windowToken, 0)
    }

    @JvmStatic
    fun setWindowSoftInputModeResizable(activity: Activity) {
        activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    fun setLightSystemUiVisibility(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!WikipediaApp.getInstance().currentTheme.isDark) {
                // this make the system recognizes the status bar is light and will make status bar icons become visible
                activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                resetSystemUiVisibility(activity)
            }
        }
    }

    private fun resetSystemUiVisibility(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.window.decorView.systemUiVisibility = 0
        }
    }

    @JvmStatic
    fun updateStatusBarTheme(activity: Activity, toolbar: Toolbar?, reset: Boolean) {
        if (reset) {
            resetSystemUiVisibility(activity)
        } else {
            setLightSystemUiVisibility(activity)
        }
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
        val info = WikipediaApp.getInstance().getSystemService<ConnectivityManager>()
                ?.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        return info != null && info.isConnected
    }

    // TODO: revisit this if there's no more navigation bar by default.
    @JvmStatic
    val isNavigationBarShowing: Boolean
        get() = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK) && KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME)

    @JvmStatic
    val isAccessibilityEnabled: Boolean
        get() {
            val am = WikipediaApp.getInstance().getSystemService<AccessibilityManager>()!!
            // TODO: add more logic if other accessibility tools have different settings.
            return am.isEnabled && am.isTouchExplorationEnabled
        }
}
