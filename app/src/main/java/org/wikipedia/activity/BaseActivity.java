package org.wikipedia.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ShortcutManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.LoginFunnel;
import org.wikipedia.appshortcuts.AppShortcuts;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.crash.CrashReportActivity;
import org.wikipedia.events.LoggedOutInBackgroundEvent;
import org.wikipedia.events.NetworkConnectEvent;
import org.wikipedia.events.ReadingListsEnableDialogEvent;
import org.wikipedia.events.ReadingListsNoLongerSyncedEvent;
import org.wikipedia.events.SplitLargeListsEvent;
import org.wikipedia.events.ThemeFontChangeEvent;
import org.wikipedia.login.LoginActivity;
import org.wikipedia.notifications.NotificationPollBroadcastReceiver;
import org.wikipedia.readinglist.ReadingListSyncBehaviorDialogs;
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter;
import org.wikipedia.readinglist.sync.ReadingListSyncEvent;
import org.wikipedia.recurring.RecurringTasksExecutor;
import org.wikipedia.savedpages.SavedPageSyncService;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.SiteInfoClient;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.PermissionUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.log.L;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;

import static org.wikipedia.Constants.INTENT_EXTRA_INVOKE_SOURCE;
import static org.wikipedia.appshortcuts.AppShortcuts.APP_SHORTCUT_ID;

public abstract class BaseActivity extends AppCompatActivity {
    private static ExclusiveBusConsumer EXCLUSIVE_BUS_METHODS;
    private static Disposable EXCLUSIVE_DISPOSABLE;

    private ExclusiveBusConsumer exclusiveBusMethods;
    private NetworkStateReceiver networkStateReceiver = new NetworkStateReceiver();
    private boolean previousNetworkState = WikipediaApp.getInstance().isOnline();
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        exclusiveBusMethods = new ExclusiveBusConsumer();
        disposables.add(WikipediaApp.getInstance().getBus().subscribe(new NonExclusiveBusConsumer()));
        setTheme();
        removeSplashBackground();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1
                && AppShortcuts.ACTION_APP_SHORTCUT.equals(getIntent().getAction())) {
            getIntent().putExtra(INTENT_EXTRA_INVOKE_SOURCE, Constants.InvokeSource.APP_SHORTCUTS);
            String shortcutId = getIntent().getStringExtra(APP_SHORTCUT_ID);
            if (!TextUtils.isEmpty(shortcutId)) {
                getApplicationContext().getSystemService(ShortcutManager.class)
                        .reportShortcutUsed(shortcutId);
            }
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        NotificationPollBroadcastReceiver.startPollTask(WikipediaApp.getInstance());

        // Conditionally execute all recurring tasks
        new RecurringTasksExecutor(WikipediaApp.getInstance()).run();

        if (Prefs.isReadingListsFirstTimeSync() && AccountUtil.isLoggedIn()) {
            Prefs.setReadingListsFirstTimeSync(false);
            Prefs.setReadingListSyncEnabled(true);
            ReadingListSyncAdapter.manualSyncWithForce();
        }

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkStateReceiver, filter);

        DeviceUtil.setLightSystemUiVisibility(this);

        setStatusBarColor(ResourceUtil.getThemedColor(this, R.attr.paper_color));
        setNavigationBarColor(ResourceUtil.getThemedColor(this, R.attr.paper_color));

        maybeShowLoggedOutInBackgroundDialog();

        Prefs.setLocalClassName(getLocalClassName());
    }

    @Override protected void onDestroy() {
        unregisterReceiver(networkStateReceiver);
        disposables.dispose();
        if (EXCLUSIVE_BUS_METHODS == exclusiveBusMethods) {
            unregisterExclusiveBusMethods();
        }
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        WikipediaApp.getInstance().getSessionFunnel().persistSession();
        super.onStop();
    }

    @Override protected void onResume() {
        super.onResume();
        WikipediaApp.getInstance().getSessionFunnel().touchSession();

        // allow this activity's exclusive bus methods to override any existing ones.
        unregisterExclusiveBusMethods();
        EXCLUSIVE_BUS_METHODS = exclusiveBusMethods;
        EXCLUSIVE_DISPOSABLE = WikipediaApp.getInstance().getBus().subscribe(EXCLUSIVE_BUS_METHODS);

        // The UI is likely shown, giving the user the opportunity to exit and making a crash loop
        // less probable.
        if (!(this instanceof CrashReportActivity)) {
            Prefs.crashedBeforeActivityCreated(false);
        }
    }

    @Override
    public void applyOverrideConfiguration(Configuration configuration) {
        // TODO: remove when this is fixed:
        // https://issuetracker.google.com/issues/141132133
        // On Lollipop the current version of AndroidX causes a crash when instantiating a WebView.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                && getResources().getConfiguration().uiMode == WikipediaApp.getInstance().getResources().getConfiguration().uiMode) {
            return;
        }
        super.applyOverrideConfiguration(configuration);
    }


    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.ACTIVITY_REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION:
                if (!PermissionUtil.isPermitted(grantResults)) {
                    L.i("Write permission was denied by user");
                    if (PermissionUtil.shouldShowWritePermissionRationale(this)) {
                        showStoragePermissionSnackbar();
                    } else {
                        showAppSettingSnackbar();
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    protected void setStatusBarColor(@ColorInt int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(color);
        }
    }

    protected void setNavigationBarColor(@ColorInt int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            boolean isDarkThemeOrDarkBackground = WikipediaApp.getInstance().getCurrentTheme().isDark()
                    || color == ContextCompat.getColor(this, android.R.color.black);
            getWindow().setNavigationBarColor(color);
            getWindow().getDecorView().setSystemUiVisibility(isDarkThemeOrDarkBackground
                    ? getWindow().getDecorView().getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    : View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | getWindow().getDecorView().getSystemUiVisibility());
        }
    }

    protected void setTheme() {
        setTheme(WikipediaApp.getInstance().getCurrentTheme().getResourceId());
    }

    protected void onGoOffline() {
    }

    protected void onGoOnline() {
    }

    private void requestStoragePermission() {
        Prefs.setAskedForPermissionOnce(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        PermissionUtil.requestWriteStorageRuntimePermissions(BaseActivity.this,
                Constants.ACTIVITY_REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION);
    }

    private void showStoragePermissionSnackbar() {
        Snackbar snackbar = FeedbackUtil.makeSnackbar(this,
                getString(R.string.offline_read_permission_rationale), FeedbackUtil.LENGTH_DEFAULT);
        snackbar.setAction(R.string.storage_access_error_retry, (v) -> requestStoragePermission());
        snackbar.show();
    }

    private void showAppSettingSnackbar() {
        Snackbar snackbar = FeedbackUtil.makeSnackbar(this,
                getString(R.string.offline_read_final_rationale), FeedbackUtil.LENGTH_DEFAULT);
        snackbar.setAction(R.string.app_settings, (v) -> goToSystemAppSettings());
        snackbar.show();
    }

    private void goToSystemAppSettings() {
        Intent appSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
        appSettings.addCategory(Intent.CATEGORY_DEFAULT);
        appSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(appSettings);
    }

    private class NetworkStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isDeviceOnline = WikipediaApp.getInstance().isOnline();

            if (isDeviceOnline) {
                if (!previousNetworkState) {
                    onGoOnline();
                }
                SavedPageSyncService.enqueue();
            } else {
                onGoOffline();
            }

            previousNetworkState = isDeviceOnline;
        }
    }

    private void removeSplashBackground() {
        getWindow().setBackgroundDrawable(null);
    }

    private void unregisterExclusiveBusMethods() {
        if (EXCLUSIVE_BUS_METHODS != null && EXCLUSIVE_DISPOSABLE != null) {
            EXCLUSIVE_DISPOSABLE.dispose();
            EXCLUSIVE_DISPOSABLE = null;
            EXCLUSIVE_BUS_METHODS = null;
        }
    }

    private void maybeShowLoggedOutInBackgroundDialog() {
        if (Prefs.wasLoggedOutInBackground()) {
            Prefs.setLoggedOutInBackground(false);
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.logged_out_in_background_title)
                    .setMessage(R.string.logged_out_in_background_dialog)
                    .setPositiveButton(R.string.logged_out_in_background_login, (dialog, which)
                            -> startActivity(LoginActivity.newIntent(BaseActivity.this, LoginFunnel.SOURCE_LOGOUT_BACKGROUND)))
                    .setNegativeButton(R.string.logged_out_in_background_cancel, null)
                    .show();
        }
    }

    /**
     * Bus consumer that should be registered by all created activities.
     */
    private class NonExclusiveBusConsumer implements Consumer<Object> {
        @Override
        public void accept(Object event) {
            if (event instanceof ThemeFontChangeEvent) {
                BaseActivity.this.recreate();
            }
        }
    }

    /**
     * Bus methods that should be caught only by the topmost activity.
     */
    private class ExclusiveBusConsumer implements Consumer<Object> {
        @Override
        public void accept(Object event) {
            if (event instanceof NetworkConnectEvent) {
                SavedPageSyncService.enqueue();
            } else if (event instanceof SplitLargeListsEvent) {
                new AlertDialog.Builder(BaseActivity.this)
                        .setMessage(getString(R.string.split_reading_list_message, SiteInfoClient.getMaxPagesPerReadingList()))
                        .setPositiveButton(R.string.reading_list_split_dialog_ok_button_text, null)
                        .show();
            } else if (event instanceof ReadingListsNoLongerSyncedEvent) {
                ReadingListSyncBehaviorDialogs.detectedRemoteTornDownDialog(BaseActivity.this);
            } else if (event instanceof ReadingListsEnableDialogEvent) {
                ReadingListSyncBehaviorDialogs.promptEnableSyncDialog(BaseActivity.this);
            } else if (event instanceof LoggedOutInBackgroundEvent) {
                maybeShowLoggedOutInBackgroundDialog();
            } else if (event instanceof ReadingListSyncEvent) {
                if (((ReadingListSyncEvent)event).showMessage() && !Prefs.isSuggestedEditsHighestPriorityEnabled()) {
                    FeedbackUtil.makeSnackbar(BaseActivity.this,
                            getString(R.string.reading_list_toast_last_sync), FeedbackUtil.LENGTH_DEFAULT).show();
                }
            }
        }
    }
}
