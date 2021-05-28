package org.wikipedia.util

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Build
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.WindowInsetsCompat
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.ktx.insetsControllerCompat

object DeviceUtil {
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
    fun setWindowSoftInputModeResizable(activity: Activity) {
        activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    @JvmStatic
    fun updateStatusBarTheme(activity: Activity, toolbar: Toolbar?, reset: Boolean) {
        activity.window.insetsControllerCompat?.isAppearanceLightStatusBars =
            !reset && !WikipediaApp.getInstance().currentTheme.isDark
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
