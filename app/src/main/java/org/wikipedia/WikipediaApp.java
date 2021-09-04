package org.wikipedia;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Window;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import org.wikipedia.analytics.FunnelManager;
import org.wikipedia.analytics.InstallReferrerListener;
import org.wikipedia.analytics.SessionFunnel;
import org.wikipedia.analytics.eventplatform.EventPlatformClient;
import org.wikipedia.appshortcuts.AppShortcuts;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.concurrency.RxBus;
import org.wikipedia.connectivity.NetworkConnectivityReceiver;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.SharedPreferenceCookieManager;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.events.ChangeTextSizeEvent;
import org.wikipedia.events.ThemeFontChangeEvent;
import org.wikipedia.language.AcceptLanguageUtil;
import org.wikipedia.language.AppLanguageState;
import org.wikipedia.notifications.NotificationCategory;
import org.wikipedia.notifications.NotificationPollBroadcastReceiver;
import org.wikipedia.page.tabs.Tab;
import org.wikipedia.push.WikipediaFirebaseMessagingService;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.RemoteConfig;
import org.wikipedia.settings.SiteInfoClient;
import org.wikipedia.theme.Theme;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.wikipedia.settings.Prefs.getTextSizeMultiplier;
import static org.wikipedia.util.DimenUtil.getFontSizeFromSp;
import static org.wikipedia.util.ReleaseUtil.getChannel;

public class WikipediaApp extends Application {
    private final RemoteConfig remoteConfig = new RemoteConfig();
    private Handler mainThreadHandler;
    private AppLanguageState appLanguageState;
    private FunnelManager funnelManager;
    private SessionFunnel sessionFunnel;
    private NetworkConnectivityReceiver connectivityReceiver = new NetworkConnectivityReceiver();
    private ActivityLifecycleHandler activityLifecycleHandler = new ActivityLifecycleHandler();
    private String userAgent;
    private WikiSite wiki;
    private RxBus bus;
    private Theme currentTheme = Theme.getFallback();
    private List<Tab> tabList = new ArrayList<>();

    private static WikipediaApp INSTANCE;

    public WikipediaApp() {
        INSTANCE = this;
    }

    public static WikipediaApp getInstance() {
        return INSTANCE;
    }

    public SessionFunnel getSessionFunnel() {
        return sessionFunnel;
    }

    public RxBus getBus() {
        return bus;
    }

    public FunnelManager getFunnelManager() {
        return funnelManager;
    }

    public RemoteConfig getRemoteConfig() {
        return remoteConfig;
    }

    /**
     * Gets the currently-selected theme for the app.
     * @return Theme that is currently selected, which is the actual theme ID that can
     * be passed to setTheme() when creating an activity.
     */
    @NonNull
    public Theme getCurrentTheme() {
        return currentTheme;
    }

    @NonNull public AppLanguageState language() {
        return appLanguageState;
    }

    @NonNull
    public String getAppOrSystemLanguageCode() {
        String code = appLanguageState.getAppLanguageCode();
        if (AccountUtil.getUserIdForLanguage(code) == 0) {
            getUserIdForLanguage(code);
        }
        return code;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        WikiSite.setDefaultBaseUrl(Prefs.getMediaWikiBaseUrl());

        // Register here rather than in AndroidManifest.xml so that we can target Android N.
        // https://developer.android.com/topic/performance/background-optimization.html#connectivity-action
        registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        LeakCanaryStubKt.setupLeakCanary();

        // See Javadocs and http://developer.android.com/tools/support-library/index.html#rev23-4-0
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        // This handler will catch exceptions thrown from Observables after they are disposed,
        // or from Observables that are (deliberately or not) missing an onError handler.
        // TODO: consider more comprehensive handling of these errors.
        RxJavaPlugins.setErrorHandler(Functions.emptyConsumer());

        bus = new RxBus();

        currentTheme = unmarshalTheme(Prefs.getCurrentThemeId());

        appLanguageState = new AppLanguageState(this);
        funnelManager = new FunnelManager(this);
        sessionFunnel = new SessionFunnel(this);

        initTabs();

        enableWebViewDebugging();

        registerActivityLifecycleCallbacks(activityLifecycleHandler);
        registerComponentCallbacks(activityLifecycleHandler);

        NotificationCategory.Companion.createNotificationChannels(this);
        AppShortcuts.Companion.setShortcuts(this);

        // Kick the notification receiver, in case it hasn't yet been started by the system.
        NotificationPollBroadcastReceiver.startPollTask(this);

        InstallReferrerListener.newInstance(this);

        // For good measure, explicitly call our token subscription function, in case the
        // API failed in previous attempts.
        WikipediaFirebaseMessagingService.Companion.updateSubscription();
        EventPlatformClient.setUpStreamConfigs();
    }

    public int getVersionCode() {
        // Our ABI-specific version codes are structured in increments of 10000, so just
        // take the actual version code modulo the increment.
        final int versionCodeMod = 10000;
        return BuildConfig.VERSION_CODE % versionCodeMod;
    }

    public String getUserAgent() {
        if (userAgent == null) {
            String channel = getChannel(this);
            channel = channel.equals("") ? channel : " ".concat(channel);
            userAgent = String.format("WikipediaApp/%s (Android %s; %s; %s Build/%s)%s",
                    BuildConfig.VERSION_NAME,
                    Build.VERSION.RELEASE,
                    getString(R.string.device_type),
                    Build.MODEL,
                    Build.ID,
                    channel
            );
        }
        return userAgent;
    }

    /**
     * @return the value that should go in the Accept-Language header.
     */
    @NonNull
    public String getAcceptLanguage(@Nullable WikiSite wiki) {
        String wikiLang = wiki == null || "meta".equals(wiki.getLanguageCode())
                ? ""
                : defaultString(wiki.getLanguageCode());
        return AcceptLanguageUtil.getAcceptLanguage(wikiLang, appLanguageState.getAppLanguageCode(),
                appLanguageState.getSystemLanguageCode());
    }

    /**
     * Default wiki for the app
     * You should use PageTitle.getWikiSite() to get the article wiki
     */
    @NonNull public synchronized WikiSite getWikiSite() {
        // TODO: why don't we ensure that the app language hasn't changed here instead of the client?
        if (wiki == null) {
            String lang = Prefs.getMediaWikiBaseUriSupportsLangCode() ? getAppOrSystemLanguageCode() : "";
            WikiSite newWiki = WikiSite.forLanguageCode(lang);
            // Kick off a task to retrieve the site info for the current wiki
            SiteInfoClient.updateFor(newWiki);
            wiki = newWiki;
            return newWiki;
        }
        return wiki;
    }

    /**
     * Get this app's unique install ID, which is a UUID that should be unique for each install
     * of the app. Useful for anonymous analytics.
     * @return Unique install ID for this app.
     */
    public String getAppInstallID() {
        String id = Prefs.getAppInstallId();
        if (id == null) {
            id = UUID.randomUUID().toString();
            Prefs.setAppInstallId(id);
        }
        return id;
    }

    /**
     * Sets the theme of the app. If the new theme is the same as the current theme, nothing happens.
     * Otherwise, an event is sent to notify of the theme change.
     */
    public void setCurrentTheme(@NonNull Theme theme) {
        if (theme != currentTheme) {
            currentTheme = theme;
            Prefs.setCurrentThemeId(currentTheme.getMarshallingId());
            bus.post(new ThemeFontChangeEvent());
        }
    }

    public boolean setFontSizeMultiplier(int multiplier) {
        int minMultiplier = getResources().getInteger(R.integer.minTextSizeMultiplier);
        int maxMultiplier = getResources().getInteger(R.integer.maxTextSizeMultiplier);
        if (multiplier < minMultiplier) {
            multiplier = minMultiplier;
        } else if (multiplier > maxMultiplier) {
            multiplier = maxMultiplier;
        }
        if (multiplier != getTextSizeMultiplier()) {
            Prefs.setTextSizeMultiplier(multiplier);
            bus.post(new ChangeTextSizeEvent());
            return true;
        }
        return false;
    }

    public void setFontFamily(@NonNull String fontFamily) {
        if (!fontFamily.equals(Prefs.getFontFamily())) {
            Prefs.setFontFamily(fontFamily);
            bus.post(new ThemeFontChangeEvent());
        }
    }

    public void putCrashReportProperty(String key, String value) {
        // TODO: add custom properties to crash report
    }

    public void logCrashManually(@NonNull Throwable throwable) {
        L.e(throwable);
        // TODO: send exception to custom crash reporting system
    }

    public Handler getMainThreadHandler() {
        if (mainThreadHandler == null) {
            mainThreadHandler = new Handler(getMainLooper());
        }
        return mainThreadHandler;
    }

    public List<Tab> getTabList() {
        return tabList;
    }

    public void commitTabState() {
        if (tabList.isEmpty()) {
            Prefs.clearTabs();
            initTabs();
        } else {
            Prefs.setTabs(tabList);
        }
    }

    public int getTabCount() {
        // handle the case where we have a single tab with an empty backstack,
        // which shouldn't count as a valid tab:
        return tabList.size() > 1 ? tabList.size()
                : tabList.isEmpty() ? 0 : tabList.get(0).getBackStack().isEmpty() ? 0 : tabList.size();
    }

    public boolean isOnline() {
        return connectivityReceiver.isOnline();
    }

    /**
     * Gets the current size of the app's font. This is given as a device-specific size (not "sp"),
     * and can be passed directly to setTextSize() functions.
     * @param window The window on which the font will be displayed.
     * @return Actual current size of the font.
     */
    public float getFontSize(Window window) {
        return getFontSizeFromSp(window,
                getResources().getDimension(R.dimen.textSize)) * (1.0f + getTextSizeMultiplier()
                * DimenUtil.getFloat(R.dimen.textSizeMultiplierFactor));
    }

    public synchronized void resetWikiSite() {
        wiki = null;
    }

    @SuppressLint("CheckResult")
    public void logOut() {
        L.d("Logging out");
        AccountUtil.removeAccount();
        Prefs.setPushNotificationTokenSubscribed(false);
        Prefs.setPushNotificationTokenOld("");
        ServiceFactory.get(getWikiSite()).getCsrfToken()
                .subscribeOn(Schedulers.io())
                .flatMap(response -> {
                    String csrfToken = response.getQuery().getCsrfToken();
                    return WikipediaFirebaseMessagingService.Companion.unsubscribePushToken(csrfToken, Prefs.getPushNotificationToken())
                            .flatMap(res -> ServiceFactory.get(getWikiSite()).postLogout(csrfToken).subscribeOn(Schedulers.io()));
                })
                .doFinally(() -> SharedPreferenceCookieManager.getInstance().clearAllCookies())
                .subscribe(response -> L.d("Logout complete."), L::e);
    }

    private void enableWebViewDebugging() {
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
    }

    public Theme unmarshalTheme(int themeId) {
        Theme result = Theme.ofMarshallingId(themeId);
        if (result == null) {
            L.d("Theme id=" + themeId + " is invalid, using fallback.");
            result = Theme.getFallback();
        }
        return result;
    }

    @SuppressLint("CheckResult")
    private void getUserIdForLanguage(@NonNull final String code) {
        if (!AccountUtil.isLoggedIn() || TextUtils.isEmpty(AccountUtil.getUserName())) {
            return;
        }
        final WikiSite wikiSite = WikiSite.forLanguageCode(code);
        ServiceFactory.get(wikiSite).getUserInfo()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    if (AccountUtil.isLoggedIn() && response.getQuery().getUserInfo() != null) {
                        // noinspection ConstantConditions
                        int id = response.getQuery().getUserInfo().getId();
                        AccountUtil.putUserIdForLanguage(code, id);
                        L.d("Found user ID " + id + " for " + code);
                    }
                }, caught -> L.e("Failed to get user ID for " + code, caught));
    }

    private void initTabs() {
        if (Prefs.hasTabs()) {
            tabList.addAll(Prefs.getTabs());
        }

        if (tabList.isEmpty()) {
            tabList.add(new Tab());
        }
    }

    public boolean haveMainActivity() {
        return activityLifecycleHandler.haveMainActivity();
    }

    public boolean isAnyActivityResumed() {
        return activityLifecycleHandler.isAnyActivityResumed();
    }
}
