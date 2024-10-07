package org.wikipedia.appshortcuts

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.main.MainActivity
import org.wikipedia.util.log.L

class AppShortcuts {
    companion object {
        const val ACTION_APP_SHORTCUT = "ACTION_APP_SHORTCUT"
        const val APP_SHORTCUT_ID = "APP_SHORTCUT_ID"
        private const val APP_SHORTCUT_ID_CONTINUE_READING = "shortcutContinueReading"
        private const val APP_SHORTCUT_ID_RANDOM = "shortcutRandom"
        private const val APP_SHORTCUT_ID_SEARCH = "shortcutSearch"
        private const val APP_SHORTCUT_ID_PLACES = "shortcutPlaces"

        fun setShortcuts(app: Context) {
            CoroutineScope(Dispatchers.Default).launch(CoroutineExceptionHandler { _, msg -> run { L.e(msg) } }) {
                val list = listOf(searchShortcut(app), continueReadingShortcut(app), randomShortcut(app), placesShortcut(app))
                if (ShortcutManagerCompat.getDynamicShortcuts(app).map { it.id }.containsAll(list.map { it.id }).not()) {
                    ShortcutManagerCompat.setDynamicShortcuts(app, list)
                } else {
                    L.d("Create dynamic shortcuts skipped.")
                }
            }
        }

        private fun searchShortcut(app: Context): ShortcutInfoCompat {
            return ShortcutInfoCompat.Builder(app, APP_SHORTCUT_ID_SEARCH)
                    .setShortLabel(app.getString(R.string.app_shortcuts_search))
                    .setLongLabel(app.getString(R.string.app_shortcuts_search))
                    .setIcon(IconCompat.createWithResource(app, R.drawable.appshortcut_ic_search))
                    .setIntent(
                            Intent(ACTION_APP_SHORTCUT, Uri.EMPTY, app, MainActivity::class.java)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    .putExtra(APP_SHORTCUT_ID, APP_SHORTCUT_ID_SEARCH)
                                    .putExtra(Constants.INTENT_APP_SHORTCUT_SEARCH, true))
                    .build()
        }

        private fun randomShortcut(app: Context): ShortcutInfoCompat {
            return ShortcutInfoCompat.Builder(app, APP_SHORTCUT_ID_RANDOM)
                    .setShortLabel(app.getString(R.string.app_shortcuts_random))
                    .setLongLabel(app.getString(R.string.app_shortcuts_random))
                    .setIcon(IconCompat.createWithResource(app, R.drawable.appshortcut_ic_random))
                    .setIntent(
                            Intent(ACTION_APP_SHORTCUT, Uri.EMPTY, app, MainActivity::class.java)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    .putExtra(APP_SHORTCUT_ID, APP_SHORTCUT_ID_RANDOM)
                                    .putExtra(Constants.INTENT_APP_SHORTCUT_RANDOMIZER, true))
                    .build()
        }

        private fun continueReadingShortcut(app: Context): ShortcutInfoCompat {
            return ShortcutInfoCompat.Builder(app, APP_SHORTCUT_ID_CONTINUE_READING)
                    .setShortLabel(app.getString(R.string.app_shortcuts_continue_reading))
                    .setLongLabel(app.getString(R.string.app_shortcuts_continue_reading))
                    .setIcon(IconCompat.createWithResource(app, R.drawable.appshortcut_ic_continue_reading))
                    .setIntent(
                            Intent(ACTION_APP_SHORTCUT, Uri.EMPTY, app, MainActivity::class.java)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    .putExtra(APP_SHORTCUT_ID, APP_SHORTCUT_ID_CONTINUE_READING)
                                    .putExtra(Constants.INTENT_APP_SHORTCUT_CONTINUE_READING, true))
                    .build()
        }

        private fun placesShortcut(app: Context): ShortcutInfoCompat {
            return ShortcutInfoCompat.Builder(app, APP_SHORTCUT_ID_PLACES)
                .setShortLabel(app.getString(R.string.app_shortcuts_places))
                .setLongLabel(app.getString(R.string.app_shortcuts_places))
                .setIcon(IconCompat.createWithResource(app, R.drawable.appshortcut_ic_places))
                .setIntent(
                    Intent(ACTION_APP_SHORTCUT, Uri.EMPTY, app, MainActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .putExtra(APP_SHORTCUT_ID, APP_SHORTCUT_ID_PLACES)
                        .putExtra(Constants.INTENT_APP_SHORTCUT_PLACES, true))
                .build()
        }
    }
}
