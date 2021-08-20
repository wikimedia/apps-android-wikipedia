package org.wikipedia.activity

import android.Manifest
import android.content.*
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.view.MotionEvent
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.pm.ShortcutManagerCompat
import com.skydoves.balloon.Balloon
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Consumer
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.LoginFunnel
import org.wikipedia.analytics.NotificationInteractionFunnel
import org.wikipedia.analytics.eventplatform.NotificationInteractionEvent
import org.wikipedia.appshortcuts.AppShortcuts
import org.wikipedia.auth.AccountUtil
import org.wikipedia.events.*
import org.wikipedia.login.LoginActivity
import org.wikipedia.main.MainActivity
import org.wikipedia.notifications.NotificationCategory
import org.wikipedia.notifications.NotificationPollBroadcastReceiver
import org.wikipedia.readinglist.ReadingListSyncBehaviorDialogs
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter
import org.wikipedia.readinglist.sync.ReadingListSyncEvent
import org.wikipedia.recurring.RecurringTasksExecutor
import org.wikipedia.savedpages.SavedPageSyncService
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.SiteInfoClient
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.PermissionUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.ImageZoomHelper

abstract class BaseActivity : AppCompatActivity() {
    private lateinit var exclusiveBusMethods: ExclusiveBusConsumer
    private val networkStateReceiver = NetworkStateReceiver()
    private var previousNetworkState = WikipediaApp.getInstance().isOnline
    private val disposables = CompositeDisposable()
    private var currentTooltip: Balloon? = null
    private var imageZoomHelper: ImageZoomHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exclusiveBusMethods = ExclusiveBusConsumer()
        disposables.add(WikipediaApp.getInstance().bus.subscribe(NonExclusiveBusConsumer()))
        setTheme()
        removeSplashBackground()
        NotificationCategory.createNotificationChannels(this)

        if (AppShortcuts.ACTION_APP_SHORTCUT == intent.action) {
            intent.putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, Constants.InvokeSource.APP_SHORTCUTS)
            val shortcutId = intent.getStringExtra(AppShortcuts.APP_SHORTCUT_ID)
            if (!shortcutId.isNullOrEmpty()) {
                ShortcutManagerCompat.reportShortcutUsed(applicationContext, shortcutId)
            }
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (savedInstanceState == null) {
            NotificationInteractionFunnel.processIntent(intent)
            NotificationInteractionEvent.processIntent(intent)
        }
        NotificationPollBroadcastReceiver.startPollTask(WikipediaApp.getInstance())

        // Conditionally execute all recurring tasks
        RecurringTasksExecutor(WikipediaApp.getInstance()).run()
        if (Prefs.isReadingListsFirstTimeSync() && AccountUtil.isLoggedIn) {
            Prefs.setReadingListsFirstTimeSync(false)
            Prefs.setReadingListSyncEnabled(true)
            ReadingListSyncAdapter.manualSyncWithForce()
        }

        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkStateReceiver, filter)

        DeviceUtil.setLightSystemUiVisibility(this)
        setStatusBarColor(ResourceUtil.getThemedColor(this, R.attr.paper_color))
        setNavigationBarColor(ResourceUtil.getThemedColor(this, R.attr.paper_color))
        maybeShowLoggedOutInBackgroundDialog()

        Prefs.setLocalClassName(localClassName)
    }

    override fun onDestroy() {
        unregisterReceiver(networkStateReceiver)
        disposables.dispose()
        if (EXCLUSIVE_BUS_METHODS === exclusiveBusMethods) {
            unregisterExclusiveBusMethods()
        }
        super.onDestroy()
    }

    override fun onStop() {
        WikipediaApp.getInstance().sessionFunnel.persistSession()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        WikipediaApp.getInstance().sessionFunnel.touchSession()

        // allow this activity's exclusive bus methods to override any existing ones.
        unregisterExclusiveBusMethods()
        EXCLUSIVE_BUS_METHODS = exclusiveBusMethods
        EXCLUSIVE_DISPOSABLE = WikipediaApp.getInstance().bus.subscribe(EXCLUSIVE_BUS_METHODS!!)

        Prefs.crashedBeforeActivityCreated(false)
    }

    override fun applyOverrideConfiguration(configuration: Configuration) {
        // TODO: remove when this is fixed:
        // https://issuetracker.google.com/issues/141132133
        // On Lollipop the current version of AndroidX causes a crash when instantiating a WebView.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M &&
                resources.configuration.uiMode == WikipediaApp.getInstance().resources.configuration.uiMode) {
            return
        }
        super.applyOverrideConfiguration(configuration)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> false
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            Constants.ACTIVITY_REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION -> if (!PermissionUtil.isPermitted(grantResults)) {
                L.i("Write permission was denied by user")
                if (PermissionUtil.shouldShowWritePermissionRationale(this)) {
                    showStoragePermissionSnackbar()
                } else {
                    showAppSettingSnackbar()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        dismissCurrentTooltip()
        imageZoomHelper?.let {
            return it.onDispatchTouchEvent(event) || super.dispatchTouchEvent(event)
        }
        return super.dispatchTouchEvent(event)
    }

    protected fun setStatusBarColor(@ColorInt color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.statusBarColor = color
        }
    }

    protected fun setNavigationBarColor(@ColorInt color: Int) {
        DeviceUtil.setNavigationBarColor(window, color)
    }

    protected open fun setTheme() {
        setTheme(WikipediaApp.getInstance().currentTheme.resourceId)
    }

    protected open fun onGoOffline() {}
    protected open fun onGoOnline() {}
    private fun requestStoragePermission() {
        Prefs.setAskedForPermissionOnce(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        PermissionUtil.requestWriteStorageRuntimePermissions(this@BaseActivity,
                Constants.ACTIVITY_REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION)
    }

    private fun showStoragePermissionSnackbar() {
        val snackbar = FeedbackUtil.makeSnackbar(this,
                getString(R.string.offline_read_permission_rationale), FeedbackUtil.LENGTH_DEFAULT)
        snackbar.setAction(R.string.storage_access_error_retry) { requestStoragePermission() }
        snackbar.show()
    }

    private fun showAppSettingSnackbar() {
        val snackbar = FeedbackUtil.makeSnackbar(this,
                getString(R.string.offline_read_final_rationale), FeedbackUtil.LENGTH_DEFAULT)
        snackbar.setAction(R.string.app_settings) { goToSystemAppSettings() }
        snackbar.show()
    }

    private fun goToSystemAppSettings() {
        val appSettings = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        appSettings.addCategory(Intent.CATEGORY_DEFAULT)
        appSettings.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(appSettings)
    }

    private inner class NetworkStateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val isDeviceOnline = WikipediaApp.getInstance().isOnline
            if (isDeviceOnline) {
                if (!previousNetworkState) {
                    onGoOnline()
                }
                SavedPageSyncService.enqueue()
            } else {
                onGoOffline()
            }
            previousNetworkState = isDeviceOnline
        }
    }

    private fun removeSplashBackground() {
        window.setBackgroundDrawable(null)
    }

    private fun unregisterExclusiveBusMethods() {
        EXCLUSIVE_DISPOSABLE?.dispose()
        EXCLUSIVE_DISPOSABLE = null
        EXCLUSIVE_BUS_METHODS = null
    }

    private fun maybeShowLoggedOutInBackgroundDialog() {
        if (Prefs.wasLoggedOutInBackground()) {
            Prefs.setLoggedOutInBackground(false)
            AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.logged_out_in_background_title)
                    .setMessage(R.string.logged_out_in_background_dialog)
                    .setPositiveButton(R.string.logged_out_in_background_login) { _: DialogInterface?, _: Int -> startActivity(LoginActivity.newIntent(this@BaseActivity, LoginFunnel.SOURCE_LOGOUT_BACKGROUND)) }
                    .setNegativeButton(R.string.logged_out_in_background_cancel, null)
                    .show()
        }
    }

    private fun dismissCurrentTooltip() {
        currentTooltip?.dismiss()
        currentTooltip = null
    }

    fun setCurrentTooltip(tooltip: Balloon) {
        dismissCurrentTooltip()
        currentTooltip = tooltip
    }

    fun setImageZoomHelper() {
        imageZoomHelper = ImageZoomHelper(this)
    }

    open fun onUnreadNotification() { }

    /**
     * Bus consumer that should be registered by all created activities.
     */
    private inner class NonExclusiveBusConsumer : Consumer<Any> {
        override fun accept(event: Any) {
            if (event is ThemeFontChangeEvent) {
                ActivityCompat.recreate(this@BaseActivity)
            }
        }
    }

    /**
     * Bus methods that should be caught only by the topmost activity.
     */
    private inner class ExclusiveBusConsumer : Consumer<Any> {
        override fun accept(event: Any) {
            if (event is NetworkConnectEvent) {
                SavedPageSyncService.enqueue()
            } else if (event is SplitLargeListsEvent) {
                AlertDialog.Builder(this@BaseActivity)
                        .setMessage(getString(R.string.split_reading_list_message, SiteInfoClient.maxPagesPerReadingList))
                        .setPositiveButton(R.string.reading_list_split_dialog_ok_button_text, null)
                        .show()
            } else if (event is ReadingListsNoLongerSyncedEvent) {
                ReadingListSyncBehaviorDialogs.detectedRemoteTornDownDialog(this@BaseActivity)
            } else if (event is ReadingListsEnableDialogEvent && this@BaseActivity is MainActivity) {
                ReadingListSyncBehaviorDialogs.promptEnableSyncDialog(this@BaseActivity)
            } else if (event is LoggedOutInBackgroundEvent) {
                maybeShowLoggedOutInBackgroundDialog()
            } else if (event is ReadingListSyncEvent) {
                if (event.showMessage && !Prefs.isSuggestedEditsHighestPriorityEnabled()) {
                    FeedbackUtil.makeSnackbar(this@BaseActivity,
                            getString(R.string.reading_list_toast_last_sync), FeedbackUtil.LENGTH_DEFAULT).show()
                }
            } else if (event is UnreadNotificationsEvent) {
                runOnUiThread {
                    if (!isDestroyed) {
                        onUnreadNotification()
                    }
                }
            }
        }
    }

    companion object {
        private var EXCLUSIVE_BUS_METHODS: ExclusiveBusConsumer? = null
        private var EXCLUSIVE_DISPOSABLE: Disposable? = null
    }
}
