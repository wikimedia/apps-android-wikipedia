package org.wikipedia

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Handler
import android.speech.RecognizerIntent
import android.text.TextUtils
import android.view.Window
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.internal.functions.Functions
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.analytics.FunnelManager
import org.wikipedia.analytics.InstallReferrerListener
import org.wikipedia.analytics.SessionFunnel
import org.wikipedia.analytics.eventplatform.EventPlatformClient
import org.wikipedia.appshortcuts.AppShortcuts
import org.wikipedia.auth.AccountUtil
import org.wikipedia.concurrency.RxBus
import org.wikipedia.connectivity.NetworkConnectivityReceiver
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.SharedPreferenceCookieManager
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.events.ChangeTextSizeEvent
import org.wikipedia.events.ThemeFontChangeEvent
import org.wikipedia.language.AcceptLanguageUtil
import org.wikipedia.language.AppLanguageState
import org.wikipedia.notifications.NotificationCategory
import org.wikipedia.notifications.NotificationPollBroadcastReceiver
import org.wikipedia.page.tabs.Tab
import org.wikipedia.push.WikipediaFirebaseMessagingService
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.SiteInfoClient
import org.wikipedia.theme.Theme
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.util.log.L
import java.util.*

class WikipediaApp : Application() {

    lateinit var mainThreadHandler: Handler
        private set
    lateinit var languageState: AppLanguageState
        private set
    lateinit var funnelManager: FunnelManager
        private set
    lateinit var sessionFunnel: SessionFunnel
        private set
    lateinit var userAgent: String
        private set

    private val connectivityReceiver = NetworkConnectivityReceiver()
    private val activityLifecycleHandler = ActivityLifecycleHandler()
    private var defaultWikiSite: WikiSite? = null

    val bus = RxBus()
    val tabList = mutableListOf<Tab>()

    var currentTheme = Theme.fallback
        set(value) {
            if (value !== field) {
                field = value
                Prefs.currentThemeId = currentTheme.marshallingId
                bus.post(ThemeFontChangeEvent())
            }
        }

    val appOrSystemLanguageCode: String
        get() {
            val code = languageState.appLanguageCode
            if (AccountUtil.getUserIdForLanguage(code) == 0) {
                getUserIdForLanguage(code)
            }
            return code
        }

    val versionCode: Int
        get() {
            // When we had ABI-specific version codes, they were structured in increments of 10000,
            // so just take the actual version code modulo the increment.
            return BuildConfig.VERSION_CODE % 10000
        }

    val appInstallID: String
        get() {
            var id = Prefs.appInstallId
            if (id == null) {
                id = UUID.randomUUID().toString()
                Prefs.appInstallId = id
            }
            return id
        }

    /**
     * Default "home" wiki for the app
     * Use PageTitle.getWikiSite() to get the article wiki
     */
    @get:Synchronized
    val wikiSite: WikiSite
        get() {
            // TODO: why don't we ensure that the app language hasn't changed here instead of the client?
            if (defaultWikiSite == null) {
                val lang = if (Prefs.mediaWikiBaseUriSupportsLangCode) appOrSystemLanguageCode else ""
                val newWiki = WikiSite.forLanguageCode(lang)
                // Kick off a task to retrieve the site info for the current wiki
                SiteInfoClient.updateFor(newWiki)
                defaultWikiSite = newWiki
            }
            return defaultWikiSite!!
        }

    // handle the case where we have a single tab with an empty backstack, which shouldn't count as a valid tab:
    val tabCount
        get() = if (tabList.size > 1) tabList.size else if (tabList.isEmpty()) 0 else if (tabList[0].backStack.isEmpty()) 0 else tabList.size

    val isOnline
        get() = connectivityReceiver.isOnline()

    val haveMainActivity
        get() = activityLifecycleHandler.haveMainActivity()

    val isAnyActivityResumed
        get() = activityLifecycleHandler.isAnyActivityResumed

    val voiceRecognitionAvailable by lazy {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()
        } catch (e: Exception) {
            L.e(e)
            false
        }
    }

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()

        WikiSite.setDefaultBaseUrl(Prefs.mediaWikiBaseUrl)

        // Register here rather than in AndroidManifest.xml so that we can target Android N.
        // https://developer.android.com/topic/performance/background-optimization.html#connectivity-action
        registerReceiver(connectivityReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        setupLeakCanary()

        // See Javadocs and http://developer.android.com/tools/support-library/index.html#rev23-4-0
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        // This handler will catch exceptions thrown from Observables after they are disposed,
        // or from Observables that are (deliberately or not) missing an onError handler.
        // TODO: consider more comprehensive handling of these errors.
        RxJavaPlugins.setErrorHandler(Functions.emptyConsumer())

        mainThreadHandler = Handler(mainLooper)

        var channel = ReleaseUtil.getChannel(this)
        channel = if (channel.isBlank()) "" else " $channel"
        userAgent = String.format("WikipediaApp/%s (Android %s; %s; %s Build/%s)%s",
                BuildConfig.VERSION_NAME,
                Build.VERSION.RELEASE,
                getString(R.string.device_type),
                Build.MODEL,
                Build.ID,
                channel
        )

        currentTheme = unmarshalTheme(Prefs.currentThemeId)
        languageState = AppLanguageState(this)
        funnelManager = FunnelManager(this)
        sessionFunnel = SessionFunnel(this)

        initTabs()
        enableWebViewDebugging()
        registerActivityLifecycleCallbacks(activityLifecycleHandler)
        registerComponentCallbacks(activityLifecycleHandler)
        NotificationCategory.createNotificationChannels(this)
        AppShortcuts.setShortcuts(this)

        // Kick the notification receiver, in case it hasn't yet been started by the system.
        NotificationPollBroadcastReceiver.startPollTask(this)
        InstallReferrerListener.newInstance(this)

        // For good measure, explicitly call our token subscription function, in case the
        // API failed in previous attempts.
        WikipediaFirebaseMessagingService.updateSubscription()

        EventPlatformClient.setUpStreamConfigs()
    }

    /**
     * @return the value that should go in the Accept-Language header.
     */
    fun getAcceptLanguage(wiki: WikiSite?): String {
        val wikiLang = if (wiki == null || "meta" == wiki.languageCode) "" else wiki.languageCode
        return AcceptLanguageUtil.getAcceptLanguage(wikiLang, languageState.appLanguageCode,
                languageState.systemLanguageCode)
    }

    fun setFontSizeMultiplier(mult: Int): Boolean {
        val multiplier = mult.coerceIn(resources.getInteger(R.integer.minTextSizeMultiplier),
            resources.getInteger(R.integer.maxTextSizeMultiplier))
        if (multiplier != Prefs.textSizeMultiplier) {
            Prefs.textSizeMultiplier = multiplier
            bus.post(ChangeTextSizeEvent())
            return true
        }
        return false
    }

    fun setFontFamily(fontFamily: String) {
        if (fontFamily != Prefs.fontFamily) {
            Prefs.fontFamily = fontFamily
            bus.post(ThemeFontChangeEvent())
        }
    }

    fun putCrashReportProperty(key: String?, value: String?) {
        // TODO: add custom properties to crash report
    }

    fun logCrashManually(throwable: Throwable) {
        L.e(throwable)
        // TODO: send exception to custom crash reporting system
    }

    fun commitTabState() {
        if (tabList.isEmpty()) {
            Prefs.clearTabs()
            initTabs()
        } else {
            Prefs.tabs = tabList
        }
    }

    /**
     * Gets the current size of the app's font. This is given as a device-specific size (not "sp"),
     * and can be passed directly to setTextSize() functions.
     * @param window The window on which the font will be displayed.
     * @return Actual current size of the font.
     */
    fun getFontSize(window: Window): Float {
        return DimenUtil.getFontSizeFromSp(window,
                resources.getDimension(R.dimen.textSize)) * (1.0f + Prefs.textSizeMultiplier
                * DimenUtil.getFloat(R.dimen.textSizeMultiplierFactor))
    }

    @Synchronized
    fun resetWikiSite() {
        defaultWikiSite = null
    }

    @SuppressLint("CheckResult")
    fun logOut() {
        L.d("Logging out")
        AccountUtil.removeAccount()
        Prefs.isPushNotificationTokenSubscribed = false
        Prefs.pushNotificationTokenOld = ""
        ServiceFactory.get(wikiSite).getTokenObservable()
                .subscribeOn(Schedulers.io())
                .flatMap {
                    val csrfToken = it.query!!.csrfToken()
                    WikipediaFirebaseMessagingService.unsubscribePushToken(csrfToken!!, Prefs.pushNotificationToken)
                            .flatMap { ServiceFactory.get(wikiSite).postLogout(csrfToken).subscribeOn(Schedulers.io()) }
                }
                .doFinally { SharedPreferenceCookieManager.getInstance().clearAllCookies() }
                .subscribe({ L.d("Logout complete.") }) { L.e(it) }
    }

    private fun enableWebViewDebugging() {
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    fun unmarshalTheme(themeId: Int): Theme {
        var result = Theme.ofMarshallingId(themeId)
        if (result == null) {
            L.d("Theme id=$themeId is invalid, using fallback.")
            result = Theme.fallback
        }
        return result
    }

    @SuppressLint("CheckResult")
    private fun getUserIdForLanguage(code: String) {
        if (!AccountUtil.isLoggedIn || TextUtils.isEmpty(AccountUtil.userName)) {
            return
        }
        val wikiSite = WikiSite.forLanguageCode(code)
        ServiceFactory.get(wikiSite).userInfo
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (AccountUtil.isLoggedIn && it.query!!.userInfo != null) {
                        // noinspection ConstantConditions
                        val id = it.query!!.userInfo!!.id
                        AccountUtil.putUserIdForLanguage(code, id)
                        L.d("Found user ID $id for $code")
                    }
                }) { L.e("Failed to get user ID for $code", it) }
    }

    private fun initTabs() {
        if (Prefs.hasTabs) {
            tabList.addAll(Prefs.tabs)
        }
        if (tabList.isEmpty()) {
            tabList.add(Tab())
        }
    }

    companion object {
        lateinit var instance: WikipediaApp
            private set
    }
}
