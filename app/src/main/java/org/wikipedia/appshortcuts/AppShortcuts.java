package org.wikipedia.appshortcuts;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.main.MainActivity;
import org.wikipedia.search.SearchActivity;
import org.wikipedia.search.SearchInvokeSource;

import java.util.Arrays;

public class AppShortcuts {

    private static final String ACTION_APP_SHORTCUT = "org.wikipedia.app_shortcut";
    private WikipediaApp app;

    public AppShortcuts() {
        app = WikipediaApp.getInstance();
    }

    @TargetApi(android.os.Build.VERSION_CODES.N_MR1)
    public void init() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            ShortcutManager shortcutManager = app.getSystemService(ShortcutManager.class);
            shortcutManager.setDynamicShortcuts(Arrays.asList(searchShortcut(), continueReadingShortcut(), randomShortcut()));
        }
    }

    @TargetApi(android.os.Build.VERSION_CODES.N_MR1)
    private ShortcutInfo randomShortcut() {

        ShortcutInfo shortcut = new ShortcutInfo.Builder(app, app.getString(R.string.app_shortcuts_random))
                .setShortLabel(app.getString(R.string.app_shortcuts_random))
                .setLongLabel(app.getString(R.string.app_shortcuts_random))
                .setIcon(Icon.createWithResource(app, R.drawable.appshortcut_ic_random))
                .setIntent(
                        new Intent(Intent.ACTION_MAIN, Uri.EMPTY, app, MainActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .putExtra(Constants.INTENT_APP_SHORTCUT_RANDOM, true))
                .build();

        return shortcut;
    }

    @TargetApi(android.os.Build.VERSION_CODES.N_MR1)
    private ShortcutInfo continueReadingShortcut() {

        ShortcutInfo shortcut = new ShortcutInfo.Builder(app, app.getString(R.string.app_shortcuts_continue_reading))
                .setShortLabel(app.getString(R.string.app_shortcuts_continue_reading))
                .setLongLabel(app.getString(R.string.app_shortcuts_continue_reading))
                .setIcon(Icon.createWithResource(app, R.drawable.appshortcut_ic_continue_reading))
                .setIntent(new Intent(ACTION_APP_SHORTCUT).putExtra(Constants.INTENT_APP_SHORTCUT_CONTINUE_READING, true))
                .build();

        return shortcut;
    }

    @TargetApi(android.os.Build.VERSION_CODES.N_MR1)
    private ShortcutInfo searchShortcut() {

        ShortcutInfo shortcut = new ShortcutInfo.Builder(app, app.getString(R.string.app_shortcuts_search))
                .setShortLabel(app.getString(R.string.app_shortcuts_search))
                .setLongLabel(app.getString(R.string.app_shortcuts_search))
                .setIcon(Icon.createWithResource(app, R.drawable.appshortcut_ic_search))
                .setIntent(SearchActivity.newIntent(app, SearchInvokeSource.APP_SHORTCUTS.code(), null).setAction(ACTION_APP_SHORTCUT))
                .build();

        return shortcut;
    }
}
