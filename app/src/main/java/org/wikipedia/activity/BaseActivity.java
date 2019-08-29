package org.wikipedia.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ShortcutManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.MenuItem;

import androidx.annotation.ColorRes;
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
import org.wikipedia.events.ReadingListsMergeLocalDialogEvent;
import org.wikipedia.events.ReadingListsNoLongerSyncedEvent;
import org.wikipedia.events.SplitLargeListsEvent;
import org.wikipedia.events.ThemeChangeEvent;
import org.wikipedia.login.LoginActivity;
import org.wikipedia.readinglist.ReadingListSyncBehaviorDialogs;
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter;
import org.wikipedia.recurring.RecurringTasksExecutor;
import org.wikipedia.savedpages.SavedPageSyncService;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.SiteInfoClient;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.PermissionUtil;
import org.wikipedia.util.log.L;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

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

        maybeShowLoggedOutInBackgroundDialog();
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

    protected void setStatusBarColor(@ColorRes int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, color));
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
            if (event instanceof ThemeChangeEvent) {
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
            } else if (event instanceof ReadingListsMergeLocalDialogEvent) {
                ReadingListSyncBehaviorDialogs.mergeExistingListsOnLoginDialog(BaseActivity.this);
            } else if (event instanceof ReadingListsEnableDialogEvent) {
                ReadingListSyncBehaviorDialogs.promptEnableSyncDialog(BaseActivity.this);
            } else if (event instanceof LoggedOutInBackgroundEvent) {
                maybeShowLoggedOutInBackgroundDialog();
            }
        }
    }
}
