package org.wikipedia;

import android.app.Activity;
import android.app.Application;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDelegate;
import android.view.Window;
import android.webkit.WebView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;
import com.squareup.otto.Bus;

import org.wikipedia.analytics.FunnelManager;
import org.wikipedia.analytics.SessionFunnel;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.concurrency.ThreadSafeBus;
import org.wikipedia.connectivity.NetworkConnectivityReceiver;
import org.wikipedia.crash.CrashReporter;
import org.wikipedia.crash.hockeyapp.HockeyAppCrashReporter;
import org.wikipedia.database.Database;
import org.wikipedia.database.DatabaseClient;
import org.wikipedia.dataclient.SharedPreferenceCookieManager;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.fresco.DisabledCache;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.okhttp.CacheableOkHttpNetworkFetcher;
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory;
import org.wikipedia.edit.summaries.EditSummary;
import org.wikipedia.events.ChangeTextSizeEvent;
import org.wikipedia.events.ThemeChangeEvent;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.language.AcceptLanguageUtil;
import org.wikipedia.language.AppLanguageState;
import org.wikipedia.login.UserIdClient;
import org.wikipedia.notifications.NotificationPollBroadcastReceiver;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.search.RecentSearch;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.RemoteConfig;
import org.wikipedia.theme.Theme;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.ReleaseUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.ViewAnimations;
import org.wikipedia.zero.WikipediaZeroHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import retrofit2.Call;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.wikipedia.settings.Prefs.getTextSizeMultiplier;
import static org.wikipedia.util.DimenUtil.getFontSizeFromSp;
import static org.wikipedia.util.ReleaseUtil.getChannel;

public class WikipediaApp extends Application {
    private final RemoteConfig remoteConfig = new RemoteConfig();
    private final Map<Class<?>, DatabaseClient<?>> databaseClients = Collections.synchronizedMap(new HashMap<Class<?>, DatabaseClient<?>>());
    private Handler mainThreadHandler;
    private AppLanguageState appLanguageState;
    private FunnelManager funnelManager;
    private SessionFunnel sessionFunnel;
    private NotificationPollBroadcastReceiver notificationReceiver = new NotificationPollBroadcastReceiver();
    private NetworkConnectivityReceiver connectivityReceiver = new NetworkConnectivityReceiver();
    private Database database;
    private String userAgent;
    private WikiSite wiki;
    private UserIdClient userIdClient = new UserIdClient();
    private CrashReporter crashReporter;
    private RefWatcher refWatcher;
    private Bus bus;
    private Theme currentTheme = Theme.getFallback();
    private WikipediaZeroHandler zeroHandler;

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

    public RefWatcher getRefWatcher() {
        return refWatcher;
    }

    public Bus getBus() {
        return bus;
    }

    public Database getDatabase() {
        return database;
    }

    public FunnelManager getFunnelManager() {
        return funnelManager;
    }

    public WikipediaZeroHandler getWikipediaZeroHandler() {
        return zeroHandler;
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

    @NonNull
    public String getAppLanguageCode() {
        return defaultString(appLanguageState.getAppLanguageCode());
    }

    @NonNull
    public String getAppOrSystemLanguageCode() {
        String code = appLanguageState.getAppOrSystemLanguageCode();
        if (AccountUtil.getUserIdForLanguage(code) == 0) {
            getUserIdForLanguage(code);
        }
        return code;
    }

    @NonNull
    public String getSystemLanguageCode() {
        return appLanguageState.getSystemLanguageCode();
    }

    public void setAppLanguageCode(@Nullable String code) {
        appLanguageState.setAppLanguageCode(code);
        resetWikiSite();
    }

    @Nullable
    public String getAppOrSystemLanguageLocalizedName() {
        return appLanguageState.getAppOrSystemLanguageLocalizedName();
    }

    @NonNull
    public List<String> getMruLanguageCodes() {
        return appLanguageState.getMruLanguageCodes();
    }

    @NonNull
    public List<String> getAppMruLanguageCodes() {
        return appLanguageState.getAppMruLanguageCodes();
    }

    public void setMruLanguageCode(@Nullable String code) {
        appLanguageState.setMruLanguageCode(code);
    }

    @Nullable
    public String getAppLanguageLocalizedName(String code) {
        return appLanguageState.getAppLanguageLocalizedName(code);
    }

    @Nullable
    public String getAppLanguageCanonicalName(String code) {
        return appLanguageState.getAppLanguageCanonicalName(code);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        zeroHandler = new WikipediaZeroHandler(this);

        // HockeyApp exception handling interferes with the test runner, so enable it only for
        // beta and stable releases
        if (!ReleaseUtil.isPreBetaRelease()) {
            initExceptionHandling();
        }

        refWatcher = Prefs.isMemoryLeakTestEnabled() ? LeakCanary.install(this) : RefWatcher.DISABLED;

        // See Javadocs and http://developer.android.com/tools/support-library/index.html#rev23-4-0
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        bus = new ThreadSafeBus();

        ViewAnimations.init(getResources());
        currentTheme = unmarshalCurrentTheme();

        appLanguageState = new AppLanguageState(this);
        funnelManager = new FunnelManager(this);
        sessionFunnel = new SessionFunnel(this);
        database = new Database(this);

        enableWebViewDebugging();

        ImagePipelineConfig config = ImagePipelineConfig.newBuilder(this)
                .setNetworkFetcher(new CacheableOkHttpNetworkFetcher(OkHttpConnectionFactory.getClient()))
                .setFileCacheFactory(DisabledCache.factory())
                .build();
        try {
            Fresco.initialize(this, config);
        } catch (Exception e) {
            L.e(e);
            // TODO: Remove when we're able to initialize Fresco in test builds.
        }

        // TODO: Remove when user accounts have been migrated to AccountManager (June 2018)
        AccountUtil.migrateAccountFromSharedPrefs();

        registerConnectivityReceiver();

        listenForNotifications();
    }

    public String getUserAgent() {
        if (userAgent == null) {
            String channel = getChannel(this);
            channel = channel.equals("") ? channel : " ".concat(channel);
            userAgent = String.format("WikipediaApp/%s (Android %s; %s)%s",
                    BuildConfig.VERSION_NAME,
                    Build.VERSION.RELEASE,
                    getString(R.string.device_type),
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
        String wikiLang = wiki == null || "meta".equals(wiki.languageCode())
                ? ""
                : defaultString(wiki.languageCode());
        return AcceptLanguageUtil.getAcceptLanguage(wikiLang, getAppLanguageCode(),
                appLanguageState.getSystemLanguageCode());
    }

    /**
     * Default wiki for the app
     * You should use PageTitle.getWikiSite() to get the article wiki
     */
    @NonNull public WikiSite getWikiSite() {
        // TODO: why don't we ensure that the app language hasn't changed here instead of the client?
        if (wiki == null) {
            String lang = Prefs.getMediaWikiBaseUriSupportsLangCode() ? getAppOrSystemLanguageCode() : "";
            wiki = WikiSite.forLanguageCode(lang);
        }
        return wiki;
    }

    public <T> DatabaseClient<T> getDatabaseClient(Class<T> cls) {
        if (!databaseClients.containsKey(cls)) {
            DatabaseClient<?> client;
            if (cls.equals(HistoryEntry.class)) {
                client = new DatabaseClient<>(this, HistoryEntry.DATABASE_TABLE);
            } else if (cls.equals(PageImage.class)) {
                client = new DatabaseClient<>(this, PageImage.DATABASE_TABLE);
            } else if (cls.equals(RecentSearch.class)) {
                client = new DatabaseClient<>(this, RecentSearch.DATABASE_TABLE);
            } else if (cls.equals(EditSummary.class)) {
                client = new DatabaseClient<>(this, EditSummary.DATABASE_TABLE);
            } else {
                throw new RuntimeException("No persister found for class " + cls.getCanonicalName());
            }
            databaseClients.put(cls, client);
        }
        //noinspection unchecked
        return (DatabaseClient<T>) databaseClients.get(cls);
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
            Prefs.setThemeId(currentTheme.getMarshallingId());
            bus.post(new ThemeChangeEvent());
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

    public void putCrashReportProperty(String key, String value) {
        if (!ReleaseUtil.isPreBetaRelease()) {
            crashReporter.putReportProperty(key, value);
        }
    }

    public void checkCrashes(@NonNull Activity activity) {
        if (!ReleaseUtil.isPreBetaRelease()) {
            crashReporter.checkCrashes(activity);
        }
    }

    public Handler getMainThreadHandler() {
        if (mainThreadHandler == null) {
            mainThreadHandler = new Handler(getMainLooper());
        }
        return mainThreadHandler;
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

    public void resetWikiSite() {
        wiki = null;
    }

    public void logOut() {
        L.v("logging out");
        AccountUtil.removeAccount();
        SharedPreferenceCookieManager.getInstance().clearAllCookies();
    }

    public void listenForNotifications() {
        if (!Prefs.suppressNotificationPolling()) {
            notificationReceiver.startPollTask(this);
        }
    }

    private void initExceptionHandling() {
        crashReporter = new HockeyAppCrashReporter(getString(R.string.hockeyapp_app_id), consentAccessor());
        crashReporter.registerCrashHandler(this);

        L.setRemoteLogger(crashReporter);
    }

    private CrashReporter.AutoUploadConsentAccessor consentAccessor() {
        return Prefs::isCrashReportAutoUploadEnabled;
    }

    private void enableWebViewDebugging() {
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
    }

    private Theme unmarshalCurrentTheme() {
        int id = Prefs.getThemeId();
        Theme result = Theme.ofMarshallingId(id);
        if (result == null) {
            L.d("Theme id=" + id + " is invalid, using fallback.");
            result = Theme.getFallback();
        }
        return result;
    }

    // Register here rather than in AndroidManifest.xml so that we can target Android N.
    // https://developer.android.com/topic/performance/background-optimization.html#connectivity-action
    private void registerConnectivityReceiver() {
        registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    private void getUserIdForLanguage(@NonNull final String code) {
        if (!AccountUtil.isLoggedIn()) {
            return;
        }
        final WikiSite wikiSite = WikiSite.forLanguageCode(code);
        userIdClient.request(wikiSite, new UserIdClient.Callback() {
            @Override
            public void success(@NonNull Call<MwQueryResponse> call, int id) {
                if (AccountUtil.isLoggedIn()) {
                    AccountUtil.putUserIdForLanguage(code, id);
                    L.v("Found user ID " + id + " for " + code);
                }
            }

            @Override
            public void failure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught) {
                L.e("Failed to get user ID for " + code, caught);
            }
        });
    }
}
