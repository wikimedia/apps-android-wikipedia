package org.wikipedia.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.view.DraweeTransition;
import com.squareup.otto.Subscribe;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.crash.CrashReportActivity;
import org.wikipedia.events.NetworkConnectEvent;
import org.wikipedia.events.ReadingListsEnableDialogEvent;
import org.wikipedia.events.ReadingListsMergeLocalDialogEvent;
import org.wikipedia.events.ReadingListsNoLongerSyncedEvent;
import org.wikipedia.events.SplitLargeListsEvent;
import org.wikipedia.events.ThemeChangeEvent;
import org.wikipedia.events.WikipediaZeroEnterEvent;
import org.wikipedia.offline.Compilation;
import org.wikipedia.offline.OfflineManager;
import org.wikipedia.readinglist.ReadingListSyncBehaviorDialogs;
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter;
import org.wikipedia.recurring.RecurringTasksExecutor;
import org.wikipedia.savedpages.SavedPageSyncService;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.PermissionUtil;
import org.wikipedia.util.log.L;

import java.util.List;

public abstract class BaseActivity extends AppCompatActivity {
    private static EventBusMethodsExclusive EXCLUSIVE_BUS_METHODS;

    private EventBusMethodsNonExclusive localBusMethods;
    private EventBusMethodsExclusive exclusiveBusMethods;
    private NetworkStateReceiver networkStateReceiver = new NetworkStateReceiver();

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        localBusMethods = new EventBusMethodsNonExclusive();
        exclusiveBusMethods = new EventBusMethodsExclusive();
        WikipediaApp.getInstance().getBus().register(localBusMethods);

        setTheme();
        removeSplashBackground();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        ActivityUtil.forceOverflowMenuIcon(this);

        // Conditionally execute all recurring tasks
        new RecurringTasksExecutor(WikipediaApp.getInstance()).run();

        if (Prefs.isReadingListsFirstTimeSync() && AccountUtil.isLoggedIn()) {
            Prefs.setReadingListsFirstTimeSync(false);
            Prefs.setReadingListSyncEnabled(true);
            ReadingListSyncAdapter.manualSyncWithForce();
        }

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkStateReceiver, filter);
    }

    @Override protected void onDestroy() {
        unregisterReceiver(networkStateReceiver);
        WikipediaApp.getInstance().getBus().unregister(localBusMethods);
        localBusMethods = null;
        if (EXCLUSIVE_BUS_METHODS == exclusiveBusMethods) {
            unregisterExclusiveBusMethods();
        }
        exclusiveBusMethods = null;
        super.onDestroy();
    }

    @Override protected void onResume() {
        super.onResume();

        // allow this activity's exclusive bus methods to override any existing ones.
        unregisterExclusiveBusMethods();
        EXCLUSIVE_BUS_METHODS = exclusiveBusMethods;
        WikipediaApp.getInstance().getBus().register(EXCLUSIVE_BUS_METHODS);

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
                if (PermissionUtil.isPermitted(grantResults)) {
                    searchOfflineCompilations(true);
                } else {
                    L.i("Write permission was denied by user");
                    onOfflineCompilationsError(new RuntimeException(getString(R.string.offline_read_permission_error)));
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, color));
        }
    }

    protected void setTheme() {
        setTheme(WikipediaApp.getInstance().getCurrentTheme().getResourceId());
    }

    protected void setSharedElementTransitions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // For using shared element transitions with Fresco, we need to explicitly define
            // a DraweeTransition that will be automatically used by Drawees that are used in
            // transitions between activities.
            getWindow().setSharedElementEnterTransition(DraweeTransition
                    .createTransitionSet(ScalingUtils.ScaleType.CENTER_CROP, ScalingUtils.ScaleType.CENTER_CROP));
            getWindow().setSharedElementReturnTransition(DraweeTransition
                    .createTransitionSet(ScalingUtils.ScaleType.CENTER_CROP, ScalingUtils.ScaleType.CENTER_CROP));
        }
    }

    protected void onGoOffline() {
    }

    protected void onGoOnline() {
    }

    protected void onOfflineCompilationsFound() {
    }

    protected void onOfflineCompilationsError(Throwable t) {
    }

    public void searchOfflineCompilationsWithPermission(boolean force) {
        if (!PermissionUtil.hasWriteExternalStoragePermission(this)) {
           requestStoragePermission();
        } else {
            searchOfflineCompilations(force);
        }
    }

    private void searchOfflineCompilations(boolean force) {
        if ((!DeviceUtil.isOnline() && OfflineManager.instance().shouldSearchAgain()) || force) {
            OfflineManager.instance().searchForCompilations(new OfflineManager.Callback() {
                @Override
                public void onCompilationsFound(@NonNull List<Compilation> compilations) {
                    if (isDestroyed()) {
                        return;
                    }
                    onOfflineCompilationsFound();
                }

                @Override
                public void onError(@NonNull Throwable t) {
                    L.e(t);
                    onOfflineCompilationsError(t);
                }
            });
        }
    }

    private void requestStoragePermission() {
        Prefs.setAskedForPermissionOnce(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        PermissionUtil.requestWriteStorageRuntimePermissions(BaseActivity.this,
                Constants.ACTIVITY_REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION);
    }

    private void showStoragePermissionSnackbar() {
        Snackbar snackbar = FeedbackUtil.makeSnackbar(this,
                getString(R.string.offline_read_permission_rationale), FeedbackUtil.LENGTH_DEFAULT);
        snackbar.setAction(R.string.page_error_retry, (v) -> requestStoragePermission());
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
            if (DeviceUtil.isOnline()) {
                onGoOnline();
                SavedPageSyncService.enqueue();
            } else {
                onGoOffline();
            }
        }
    }

    private void removeSplashBackground() {
        getWindow().setBackgroundDrawable(null);
    }

    private void unregisterExclusiveBusMethods() {
        if (EXCLUSIVE_BUS_METHODS != null) {
            WikipediaApp.getInstance().getBus().unregister(EXCLUSIVE_BUS_METHODS);
            EXCLUSIVE_BUS_METHODS = null;
        }
    }

    /**
     * Bus methods that should be caught by all created activities.
     */
    private class EventBusMethodsNonExclusive {
        @Subscribe public void on(ThemeChangeEvent event) {
            recreate();
        }
    }

    /**
     * Bus methods that should be caught only by the topmost activity.
     */
    private class EventBusMethodsExclusive {
        // todo: reevaluate lifecycle. the bus is active when this activity is paused and we show ui
        @Subscribe public void on(WikipediaZeroEnterEvent event) {
            if (Prefs.isZeroTutorialEnabled()) {
                Prefs.setZeroTutorialEnabled(false);
                WikipediaApp.getInstance().getWikipediaZeroHandler()
                        .showZeroTutorialDialog(BaseActivity.this);
            }
        }

        @Subscribe public void on(NetworkConnectEvent event) {
            SavedPageSyncService.enqueue();
        }

        @Subscribe public void on(SplitLargeListsEvent event) {
            new AlertDialog.Builder(BaseActivity.this)
                    .setMessage(getString(R.string.split_reading_list_message, Constants.MAX_READING_LIST_ARTICLE_LIMIT))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }

        @Subscribe public void on(ReadingListsNoLongerSyncedEvent event) {
            ReadingListSyncBehaviorDialogs.detectedRemoteTornDownDialog(BaseActivity.this);
        }

        @Subscribe public void on(ReadingListsMergeLocalDialogEvent event) {
            ReadingListSyncBehaviorDialogs.mergeExistingListsOnLoginDialog(BaseActivity.this);
        }

        @Subscribe public void on(ReadingListsEnableDialogEvent event) {
            ReadingListSyncBehaviorDialogs.promptEnableSyncDialog(BaseActivity.this);
        }
    }
}
