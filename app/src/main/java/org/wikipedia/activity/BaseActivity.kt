package org.wikipedia.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.TextView
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
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent
import org.wikipedia.analytics.eventplatform.NotificationInteractionEvent
import org.wikipedia.appshortcuts.AppShortcuts
import org.wikipedia.auth.AccountUtil
import org.wikipedia.events.*
import org.wikipedia.login.LoginActivity
import org.wikipedia.main.MainActivity
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.readinglist.ReadingListSyncBehaviorDialogs
import org.wikipedia.readinglist.ReadingListsReceiveSurveyHelper
import org.wikipedia.readinglist.ReadingListsShareSurveyHelper
import org.wikipedia.readinglist.ReadingListsSurveyHelper
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter
import org.wikipedia.readinglist.sync.ReadingListSyncEvent
import org.wikipedia.recurring.RecurringTasksExecutor
import org.wikipedia.savedpages.SavedPageSyncService
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.SiteInfoClient
import org.wikipedia.util.*
import org.wikipedia.views.ImageZoomHelper
import org.wikipedia.views.ViewUtil
import kotlin.math.abs

abstract class BaseActivity : AppCompatActivity() {
    private lateinit var exclusiveBusMethods: ExclusiveBusConsumer
    private val networkStateReceiver = NetworkStateReceiver()
    private var previousNetworkState = WikipediaApp.instance.isOnline
    private val disposables = CompositeDisposable()
    private var currentTooltip: Balloon? = null
    private var imageZoomHelper: ImageZoomHelper? = null

    private var startTouchX = 0f
    private var startTouchY = 0f
    private var startTouchMillis = 0L
    private var touchSlopPx = 0

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

        // Conditionally execute all recurring tasks
        RecurringTasksExecutor(WikipediaApp.instance).run()
        if (Prefs.isReadingListsFirstTimeSync && AccountUtil.isLoggedIn) {
            Prefs.isReadingListsFirstTimeSync = false
            Prefs.isReadingListSyncEnabled = true
            ReadingListSyncAdapter.manualSyncWithForce()
        }

        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkStateReceiver, filter)

        DeviceUtil.setLightSystemUiVisibility(this)
        setStatusBarColor(ResourceUtil.getThemedColor(this, R.attr.paper_color))
        setNavigationBarColor(ResourceUtil.getThemedColor(this, R.attr.paper_color))
        maybeShowLoggedOutInBackgroundDialog()

        ReadingListsShareSurveyHelper.maybeShowSurvey(this)
        ReadingListsReceiveSurveyHelper.maybeShowSurvey(this)

        touchSlopPx = ViewConfiguration.get(this).scaledTouchSlop
        Prefs.localClassName = localClassName
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        BreadCrumbLogEvent.logScreenShown(this)
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
        WikipediaApp.instance.sessionFunnel.persistSession()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        WikipediaApp.instance.sessionFunnel.touchSession()

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

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startTouchMillis = System.currentTimeMillis()
                startTouchX = event.x
                startTouchY = event.y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val touchMillis = System.currentTimeMillis() - startTouchMillis
                val dx = abs(startTouchX - event.x)
                val dy = abs(startTouchY - event.y)

                if (dx <= touchSlopPx && dy <= touchSlopPx) {
                    ViewUtil.findClickableViewAtPoint(window.decorView, startTouchX.toInt(), startTouchY.toInt())?.let {
                        if (it is TextView && it.movementMethod is LinkMovementMethodExt) {
                            // If they clicked a link in a TextView, it will be handled by the
                            // MovementMethod instead of here.
                        } else {
                            if (touchMillis > ViewConfiguration.getLongPressTimeout()) {
                                BreadCrumbLogEvent.logLongClick(this@BaseActivity, it)
                            } else {
                                BreadCrumbLogEvent.logClick(this@BaseActivity, it)
                            }
                        }
                    }
                }
            }
        }

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
        setTheme(WikipediaApp.instance.currentTheme.resourceId)
    }

    protected open fun onGoOffline() {}
    protected open fun onGoOnline() {}

    private inner class NetworkStateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val isDeviceOnline = WikipediaApp.instance.isOnline
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
        if (Prefs.loggedOutInBackground) {
            Prefs.loggedOutInBackground = false
            AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.logged_out_in_background_title)
                    .setMessage(R.string.logged_out_in_background_dialog)
                    .setPositiveButton(R.string.logged_out_in_background_login) { _, _ -> startActivity(LoginActivity.newIntent(this@BaseActivity, LoginFunnel.SOURCE_LOGOUT_BACKGROUND)) }
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
