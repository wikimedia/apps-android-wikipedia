package org.wikipedia.activity

import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.pm.ShortcutManagerCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.skydoves.balloon.Balloon
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Consumer
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.BreadcrumbsContextHelper
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent
import org.wikipedia.analytics.eventplatform.NotificationInteractionEvent
import org.wikipedia.appshortcuts.AppShortcuts
import org.wikipedia.auth.AccountUtil
import org.wikipedia.connectivity.ConnectionStateMonitor
import org.wikipedia.events.*
import org.wikipedia.login.LoginActivity
import org.wikipedia.main.MainActivity
import org.wikipedia.readinglist.ReadingListSyncBehaviorDialogs
import org.wikipedia.readinglist.ReadingListsReceiveSurveyHelper
import org.wikipedia.readinglist.ReadingListsShareSurveyHelper
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter
import org.wikipedia.readinglist.sync.ReadingListSyncEvent
import org.wikipedia.recurring.RecurringTasksExecutor
import org.wikipedia.richtext.CustomHtmlParser
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.SiteInfoClient
import org.wikipedia.theme.Theme
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.ImageZoomHelper

abstract class BaseActivity : AppCompatActivity(), ConnectionStateMonitor.Callback {
    interface Callback {
        fun onPermissionResult(activity: BaseActivity, isGranted: Boolean)
    }
    private lateinit var exclusiveBusMethods: ExclusiveBusConsumer
    private val disposables = CompositeDisposable()
    private var currentTooltip: Balloon? = null
    private var imageZoomHelper: ImageZoomHelper? = null
    var callback: Callback? = null

    val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            callback?.onPermissionResult(this, isGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exclusiveBusMethods = ExclusiveBusConsumer()
        disposables.add(WikipediaApp.instance.bus.subscribe(NonExclusiveBusConsumer()))
        setTheme()
        removeSplashBackground()

        if (AppShortcuts.ACTION_APP_SHORTCUT == intent.action) {
            intent.putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, Constants.InvokeSource.APP_SHORTCUTS)
            val shortcutId = intent.getStringExtra(AppShortcuts.APP_SHORTCUT_ID)
            if (!shortcutId.isNullOrEmpty()) {
                ShortcutManagerCompat.reportShortcutUsed(applicationContext, shortcutId)
            }
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (savedInstanceState == null) {
            NotificationInteractionEvent.processIntent(intent)
        }

        // Schedule all recurring tasks
        RecurringTasksExecutor.scheduleTasks(this)
        if (Prefs.isReadingListsFirstTimeSync && AccountUtil.isLoggedIn) {
            Prefs.isReadingListsFirstTimeSync = false
            Prefs.isReadingListSyncEnabled = true
            ReadingListSyncAdapter.manualSyncWithForce()
        }

        WikipediaApp.instance.connectionStateMonitor.registerCallback(this)

        DeviceUtil.setLightSystemUiVisibility(this)
        setStatusBarColor(ResourceUtil.getThemedColor(this, R.attr.paper_color))
        setNavigationBarColor(ResourceUtil.getThemedColor(this, R.attr.paper_color))
        maybeShowLoggedOutInBackgroundDialog()

        if (ReadingListsShareSurveyHelper.shouldShowSurvey(this)) {
            ReadingListsShareSurveyHelper.maybeShowSurvey(this)
        } else {
            ReadingListsReceiveSurveyHelper.maybeShowSurvey(this)
        }

        Prefs.localClassName = localClassName
    }

    override fun onDestroy() {
        WikipediaApp.instance.connectionStateMonitor.unregisterCallback(this)
        disposables.dispose()
        if (EXCLUSIVE_BUS_METHODS === exclusiveBusMethods) {
            unregisterExclusiveBusMethods()
        }
        CustomHtmlParser.pruneBitmaps(this)
        super.onDestroy()
    }

    override fun onStop() {
        WikipediaApp.instance.appSessionEvent.persistSession()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        WikipediaApp.instance.appSessionEvent.touchSession()
        BreadCrumbLogEvent.logScreenShown(this)

        // allow this activity's exclusive bus methods to override any existing ones.
        unregisterExclusiveBusMethods()
        EXCLUSIVE_BUS_METHODS = exclusiveBusMethods
        EXCLUSIVE_DISPOSABLE = WikipediaApp.instance.bus.subscribe(EXCLUSIVE_BUS_METHODS!!)
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

    override fun onBackPressed() {
        super.onBackPressed()
        BreadCrumbLogEvent.logBackPress(this)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN ||
                event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
            dismissCurrentTooltip()
        }

        BreadcrumbsContextHelper.dispatchTouchEvent(window, event)

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
        if (WikipediaApp.instance.currentTheme != Theme.LIGHT) {
            setTheme(WikipediaApp.instance.currentTheme.resourceId)
        }
    }

    override fun onGoOffline() {}

    override fun onGoOnline() {}

    private fun removeSplashBackground() {
        window.setBackgroundDrawable(ColorDrawable(ResourceUtil.getThemedColor(this, R.attr.paper_color)))
    }

    private fun unregisterExclusiveBusMethods() {
        EXCLUSIVE_DISPOSABLE?.dispose()
        EXCLUSIVE_DISPOSABLE = null
        EXCLUSIVE_BUS_METHODS = null
    }

    private fun maybeShowLoggedOutInBackgroundDialog() {
        if (Prefs.loggedOutInBackground) {
            Prefs.loggedOutInBackground = false
            MaterialAlertDialogBuilder(this)
                    .setCancelable(false)
                    .setTitle(R.string.logged_out_in_background_title)
                    .setMessage(R.string.logged_out_in_background_dialog)
                    .setPositiveButton(R.string.logged_out_in_background_login) { _, _ -> startActivity(LoginActivity.newIntent(this@BaseActivity, LoginActivity.SOURCE_LOGOUT_BACKGROUND)) }
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
            if (event is SplitLargeListsEvent) {
                MaterialAlertDialogBuilder(this@BaseActivity)
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
                if (event.showMessage && !Prefs.isSuggestedEditsHighestPriorityEnabled) {
                    FeedbackUtil.makeSnackbar(this@BaseActivity, getString(R.string.reading_list_toast_last_sync)).show()
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
