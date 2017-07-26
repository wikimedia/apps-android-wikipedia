package org.wikipedia.activity;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;

import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.view.DraweeTransition;
import com.squareup.otto.Subscribe;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.events.NetworkConnectEvent;
import org.wikipedia.events.WikipediaZeroEnterEvent;
import org.wikipedia.offline.Compilation;
import org.wikipedia.offline.OfflineManager;
import org.wikipedia.readinglist.sync.ReadingListSynchronizer;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.PermissionUtil;
import org.wikipedia.util.ReleaseUtil;
import org.wikipedia.util.log.L;

import java.util.List;

public abstract class BaseActivity extends AppCompatActivity {
    private boolean destroyed;
    private EventBusMethods busMethods;
    private NetworkStateReceiver networkStateReceiver = new NetworkStateReceiver();

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return false;
        }
    }

    protected void requestFullUserOrientation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
        }
    }

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        busMethods = new EventBusMethods();
        WikipediaApp.getInstance().getBus().register(busMethods);
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkStateReceiver, filter);
    }

    @Override protected void onDestroy() {
        unregisterReceiver(networkStateReceiver);
        WikipediaApp.getInstance().getBus().unregister(busMethods);
        busMethods = null;
        super.onDestroy();
        destroyed = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.ACTIVITY_REQUEST_READ_EXTERNAL_STORAGE_PERMISSION_OFFLINE:
                if (PermissionUtil.isPermitted(grantResults)) {
                    searchOfflineCompilations(true);
                } else {
                    L.i("Read permission was denied by user");
                    onOfflineCompilationsError(new RuntimeException(getString(R.string.offline_read_permission_error)));
                    if (PermissionUtil.shouldShowReadPermissionRationale(this)) {
                        showReadPermissionSnackbar();
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

    @Override public boolean isDestroyed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return super.isDestroyed();
        }
        return destroyed;
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

    protected void searchOfflineCompilationsWithPermission(boolean force) {
        if (!ReleaseUtil.isPreBetaRelease()) {
            // TODO: enable when ready for production.
            return;
        }
        if (!PermissionUtil.hasReadExternalStoragePermission(this)) {
            if (PermissionUtil.shouldShowReadPermissionRationale(this)) {
                requestReadPermission();
            } else {
                onOfflineCompilationsError(new RuntimeException(getString(R.string.offline_read_permission_error)));
            }
        } else {
            searchOfflineCompilations(force);
        }
    }

    @TargetApi(17)
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

    private void requestReadPermission() {
        PermissionUtil.requestReadStorageRuntimePermissions(BaseActivity.this,
                Constants.ACTIVITY_REQUEST_READ_EXTERNAL_STORAGE_PERMISSION_OFFLINE);
    }

    private void showReadPermissionSnackbar() {
        Snackbar snackbar = FeedbackUtil.makeSnackbar(this,
                getString(R.string.offline_read_permission_rationale), FeedbackUtil.LENGTH_DEFAULT);
        snackbar.setAction(R.string.page_error_retry, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestReadPermission();
            }
        });
        snackbar.show();
    }

    private class NetworkStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DeviceUtil.isOnline()) {
                onGoOnline();
            } else {
                onGoOffline();
            }
        }
    }

    private class EventBusMethods {
        // todo: reevaluate lifecycle. the bus is active when this activity is paused and we show ui
        @Subscribe public void on(WikipediaZeroEnterEvent event) {
            if (Prefs.isZeroTutorialEnabled()) {
                Prefs.setZeroTutorialEnabled(false);
                WikipediaApp.getInstance().getWikipediaZeroHandler()
                        .showZeroTutorialDialog(BaseActivity.this);
            }
        }

        @Subscribe public void on(NetworkConnectEvent event) {
            ReadingListSynchronizer.instance().syncSavedPages();
        }
    }
}
