package org.wikipedia

import android.annotation.SuppressLint
import android.app.Application
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import android.os.Handler
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
import org.wikipedia.auth.AccountUtil
import org.wikipedia.concurrency.RxBus
import org.wikipedia.connectivity.NetworkConnectivityReceiver
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.SharedPreferenceCookieManager
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.events.ChangeTextSizeEvent
import org.wikipedia.events.ThemeFontChangeEvent
import org.wikipedia.language.AcceptLanguageUtil
import org.wikipedia.language.AppLanguageState
import org.wikipedia.notifications.NotificationPollBroadcastReceiver
import org.wikipedia.page.tabs.Tab
import org.wikipedia.push.WikipediaFirebaseMessagingService
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig
import org.wikipedia.settings.SiteInfoClient.updateFor
import org.wikipedia.theme.Theme
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.util.log.L
import java.util.*

class WikipediaApp : Application() {
    lateinit var appLanguageState: AppLanguageState
    lateinit var funnelManager: FunnelManager
    lateinit var sessionFunnel: SessionFunnel
    lateinit var bus: RxBus
        private set

    val remoteConfig = RemoteConfig()
    val mainThreadHandler: Handler by lazy { Handler(mainLooper) }
    val userAgent: String by lazy {
        var channel = ReleaseUtil.getChannel(this)
        channel = if (channel.isEmpty()) "" else " $channel"
        String.format(
            "WikipediaApp/%s (Android %s; %s; %s Build/%s)%s",
            BuildConfig.VERSION_NAME,
            Build.VERSION.RELEASE,
            getString(R.string.device_type),
            Build.MODEL,
            Build.ID,
            channel
        )
    }

    private val connectivityReceiver = NetworkConnectivityReceiver()
    private val activityLifecycleHandler = ActivityLifecycleHandler()
    private var wiki: WikiSite? = null
    val tabList: MutableList<Tab> = ArrayList()

    var currentTheme = Theme.fallback
        set(value) {
            if (value !== field) {
                field = value
                Prefs.setCurrentThemeId(currentTheme.marshallingId)
                bus.post(ThemeFontChangeEvent())
            }
        }

    val appOrSystemLanguageCode: String
        get() {
            val code = appLanguageState.appLanguageCode
            if (AccountUtil.getUserIdForLanguage(code) == 0) {
                getUserIdForLanguage(code)
            }
            return code
        }

    val isAnyActivityResumed: Boolean
        get() = activityLifecycleHandler.isAnyActivityResumed

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()
        WikiSite.setDefaultBaseUrl(Prefs.getMediaWikiBaseUrl())

        // Register here rather than in AndroidManifest.xml so that we can target Android N.
        // https://developer.android.com/topic/performance/background-optimization.html#connectivity-action
        registerReceiver(
            connectivityReceiver,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )
        setupLeakCanary()

        // See Javadocs and http://developer.android.com/tools/support-library/index.html#rev23-4-0
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        // This handler will catch exceptions thrown from Observables after they are disposed,
        // or from Observables that are (deliberately or not) missing an onError handler.
        // TODO: consider more comprehensive handling of these errors.
        RxJavaPlugins.setErrorHandler(Functions.emptyConsumer())
        bus = RxBus()
        currentTheme = unmarshalTheme(Prefs.getCurrentThemeId())
        appLanguageState = AppLanguageState(this)
        funnelManager = FunnelManager(this)
        sessionFunnel = SessionFunnel(this)
        initTabs()
        enableWebViewDebugging()
        registerActivityLifecycleCallbacks(activityLifecycleHandler)
        registerComponentCallbacks(activityLifecycleHandler)

        // Kick the notification receiver, in case it hasn't yet been started by the system.
        NotificationPollBroadcastReceiver.startPollTask(this)
        InstallReferrerListener.newInstance(this)

        // For good measure, explicitly call our token subscription function, in case the
        // API failed in previous attempts.
        WikipediaFirebaseMessagingService.updateSubscription()
        EventPlatformClient.setUpStreamConfigs()
    }

    // Our ABI-specific version codes are structured in increments of 10000, so just
    // take the actual version code modulo the increment.
    val versionCode: Int
        get() {
            // Our ABI-specific version codes are structured in increments of 10000, so just
            // take the actual version code modulo the increment.
            val versionCodeMod = 10000
            return BuildConfig.VERSION_CODE % versionCodeMod
        }

    /**
     * @return the value that should go in the Accept-Language header.
     */
    fun getAcceptLanguage(wiki: WikiSite?): String {
        val wikiLang = if (wiki == null || "meta" == wiki.languageCode) "" else wiki.languageCode
        return AcceptLanguageUtil.getAcceptLanguage(
            wikiLang, appLanguageState.appLanguageCode,
            appLanguageState.systemLanguageCode
        )
    }

    /**
     * Default wiki for the app
     * You should use PageTitle.getWikiSite() to get the article wiki
     */
    @get:Synchronized
    val wikiSite: WikiSite
        get() {
            // TODO: why don't we ensure that the app language hasn't changed here instead of the client?
            return wiki ?: run {
                val lang =
                    if (Prefs.getMediaWikiBaseUriSupportsLangCode()) appOrSystemLanguageCode else ""
                val newWiki = WikiSite.forLanguageCode(lang)
                // Kick off a task to retrieve the site info for the current wiki
                updateFor(newWiki)
                wiki = newWiki
                newWiki
            }
        }

    /**
     * Get this app's unique install ID, which is a UUID that should be unique for each install
     * of the app. Useful for anonymous analytics.
     * @return Unique install ID for this app.
     */
    val appInstallID: String
        get() = Prefs.getAppInstallId() ?: UUID.randomUUID().toString().also { Prefs.setAppInstallId(it) }

    fun setFontSizeMultiplier(multiplier: Int): Boolean {
        val minMultiplier = resources.getInteger(R.integer.minTextSizeMultiplier)
        val maxMultiplier = resources.getInteger(R.integer.maxTextSizeMultiplier)
        val multiplier = multiplier.coerceIn(minMultiplier, maxMultiplier)
        if (multiplier != Prefs.getTextSizeMultiplier()) {
            Prefs.setTextSizeMultiplier(multiplier)
            bus.post(ChangeTextSizeEvent())
            return true
        }
        return false
    }

    fun setFontFamily(fontFamily: String) {
        if (fontFamily != Prefs.getFontFamily()) {
            Prefs.setFontFamily(fontFamily)
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
            Prefs.setTabs(tabList)
        }
    }

    // handle the case where we have a single tab with an empty backstack,
    // which shouldn't count as a valid tab:
    val tabCount: Int
        get() = if (tabList.size > 1 || !tabList.firstOrNull()?.backStack.isNullOrEmpty()) tabList.size else 0
    val isOnline: Boolean
        get() = connectivityReceiver.isOnline()

    /**
     * Gets the current size of the app's font. This is given as a device-specific size (not "sp"),
     * and can be passed directly to setTextSize() functions.
     * @param window The window on which the font will be displayed.
     * @return Actual current size of the font.
     */
    fun getFontSize(window: Window): Float {
        return DimenUtil.getFontSizeFromSp(window, resources.getDimension(R.dimen.textSize)) *
                (1.0f + Prefs.getTextSizeMultiplier() * DimenUtil.getFloat(R.dimen.textSizeMultiplierFactor))
    }

    @Synchronized
    fun resetWikiSite() {
        wiki = null
    }

    @SuppressLint("CheckResult")
    fun logOut() {
        L.d("Logging out")
        AccountUtil.removeAccount()
        Prefs.setPushNotificationTokenSubscribed(false)
        Prefs.setPushNotificationTokenOld("")
        ServiceFactory.get(wikiSite).csrfToken
            .subscribeOn(Schedulers.io())
            .flatMap { response: MwQueryResponse ->
                val csrfToken = response.query!!.csrfToken()
                WikipediaFirebaseMessagingService.unsubscribePushToken(csrfToken!!, Prefs.getPushNotificationToken())
                    .flatMap {
                        ServiceFactory.get(wikiSite).postLogout(csrfToken).subscribeOn(Schedulers.io())
                    }
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
        if (!AccountUtil.isLoggedIn || AccountUtil.userName.isNullOrEmpty()) {
            return
        }
        val wikiSite = WikiSite.forLanguageCode(code)
        ServiceFactory.get(wikiSite).userInfo
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response: MwQueryResponse ->
                if (AccountUtil.isLoggedIn && response.query!!.userInfo != null) {
                    // noinspection ConstantConditions
                    val id = response.query!!.userInfo!!.id
                    AccountUtil.putUserIdForLanguage(code, id)
                    L.d("Found user ID $id for $code")
                }
            }) { caught: Throwable? -> L.e("Failed to get user ID for $code", caught) }
    }

    private fun initTabs() {
        if (Prefs.hasTabs()) {
            tabList.addAll(Prefs.getTabs())
        }
        if (tabList.isEmpty()) {
            tabList.add(Tab())
        }
    }

    fun haveMainActivity(): Boolean {
        return activityLifecycleHandler.haveMainActivity()
    }

    companion object {
        @JvmStatic
        lateinit var instance: WikipediaApp
            private set
    }
}
