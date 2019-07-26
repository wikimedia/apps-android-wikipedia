package org.wikipedia.appshortcuts

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.util.log.L

class AppShortcuts {
    companion object {
        const val ACTION_APP_SHORTCUT = "ACTION_APP_SHORTCUT"

        @JvmStatic
        fun setShortcuts(app: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                CoroutineScope(Dispatchers.Main).launch(CoroutineExceptionHandler { _, exception -> run { L.e(exception) } }) {
                    app.getSystemService(ShortcutManager::class.java)
                            .dynamicShortcuts = listOf(continueReadingShortcut(app))
                }
            }
        }

        @TargetApi(Build.VERSION_CODES.N_MR1)
        private fun continueReadingShortcut(app: Context): ShortcutInfo {
            return ShortcutInfo.Builder(app, app.getString(R.string.app_shortcuts_continue_reading))
                    .setShortLabel(app.getString(R.string.app_shortcuts_continue_reading))
                    .setLongLabel(app.getString(R.string.app_shortcuts_continue_reading))
                    .setIcon(Icon.createWithResource(app, R.drawable.appshortcut_ic_continue_reading))
                    .setIntent(Intent(ACTION_APP_SHORTCUT).putExtra(Constants.INTENT_APP_SHORTCUT_CONTINUE_READING, true))
                    .build()
        }
    }
}
