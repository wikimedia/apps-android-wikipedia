package org.wikipedia.appshortcuts

import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.main.MainActivity
import org.wikipedia.search.SearchActivity
import org.wikipedia.search.SearchInvokeSource
import java.util.*

class AppShortcuts {
    private val app: WikipediaApp = WikipediaApp.getInstance()

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            app.getSystemService(ShortcutManager::class.java)
                    .dynamicShortcuts = Arrays.asList(searchShortcut(), continueReadingShortcut(), randomShortcut())
        }
    }

    @TargetApi(android.os.Build.VERSION_CODES.N_MR1)
    private fun randomShortcut(): ShortcutInfo {
        return ShortcutInfo.Builder(app, app.getString(R.string.app_shortcuts_random))
                .setShortLabel(app.getString(R.string.app_shortcuts_random))
                .setLongLabel(app.getString(R.string.app_shortcuts_random))
                .setIcon(Icon.createWithResource(app, R.drawable.appshortcut_ic_random))
                .setIntent(
                        Intent(Intent.ACTION_MAIN, Uri.EMPTY, app, MainActivity::class.java)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .putExtra(Constants.INTENT_APP_SHORTCUT_RANDOM, true))
                .build()
    }

    @TargetApi(android.os.Build.VERSION_CODES.N_MR1)
    private fun continueReadingShortcut(): ShortcutInfo {
        return ShortcutInfo.Builder(app, app.getString(R.string.app_shortcuts_continue_reading))
                .setShortLabel(app.getString(R.string.app_shortcuts_continue_reading))
                .setLongLabel(app.getString(R.string.app_shortcuts_continue_reading))
                .setIcon(Icon.createWithResource(app, R.drawable.appshortcut_ic_continue_reading))
                .setIntent(Intent(ACTION_APP_SHORTCUT).putExtra(Constants.INTENT_APP_SHORTCUT_CONTINUE_READING, true))
                .build()
    }

    @TargetApi(android.os.Build.VERSION_CODES.N_MR1)
    private fun searchShortcut(): ShortcutInfo {
        return ShortcutInfo.Builder(app, app.getString(R.string.app_shortcuts_search))
                .setShortLabel(app.getString(R.string.app_shortcuts_search))
                .setLongLabel(app.getString(R.string.app_shortcuts_search))
                .setIcon(Icon.createWithResource(app, R.drawable.appshortcut_ic_search))
                .setIntent(SearchActivity.newIntent(app, SearchInvokeSource.APP_SHORTCUTS.code(), null).setAction(ACTION_APP_SHORTCUT))
                .build()
    }

    companion object {
        const val ACTION_APP_SHORTCUT = "org.wikipedia.app_shortcut"
    }
}
