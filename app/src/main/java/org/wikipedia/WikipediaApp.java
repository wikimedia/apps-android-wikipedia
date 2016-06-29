package org.wikipedia;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDelegate;
import android.view.Window;
import android.webkit.WebView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.backends.okhttp3.OkHttpImagePipelineConfigFactory;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.squareup.otto.Bus;

import org.mediawiki.api.json.Api;
import org.wikipedia.analytics.FunnelManager;
import org.wikipedia.analytics.SessionFunnel;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.crash.CrashReporter;
import org.wikipedia.crash.hockeyapp.HockeyAppCrashReporter;
import org.wikipedia.database.Database;
import org.wikipedia.database.DatabaseClient;
import org.wikipedia.database.contract.AppContentProviderContract;
import org.wikipedia.database.contract.ReadingListPageContract;
import org.wikipedia.editing.EditTokenStorage;
import org.wikipedia.editing.summaries.EditSummary;
import org.wikipedia.events.ChangeTextSizeEvent;
import org.wikipedia.events.ThemeChangeEvent;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.interlanguage.AcceptLanguageUtil;
import org.wikipedia.interlanguage.AppLanguageState;
import org.wikipedia.login.UserInfoStorage;
import org.wikipedia.onboarding.OnboardingStateMachine;
import org.wikipedia.onboarding.PrefsOnboardingStateMachine;
import org.wikipedia.page.PageCache;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.readinglist.database.ReadingListRow;
import org.wikipedia.readinglist.page.ReadingListPageRow;
import org.wikipedia.readinglist.page.database.ReadingListPageHttpRow;
import org.wikipedia.readinglist.page.database.disk.ReadingListPageDiskRow;
import org.wikipedia.savedpages.ReadingListPageObserver;
import org.wikipedia.savedpages.SavedPage;
import org.wikipedia.search.RecentSearch;
import org.wikipedia.settings.Prefs;
import org.wikipedia.theme.Theme;
import org.wikipedia.useroption.UserOption;
import org.wikipedia.useroption.database.UserOptionDao;
import org.wikipedia.useroption.database.UserOptionRow;
import org.wikipedia.useroption.sync.UserOptionContentResolver;
import org.wikipedia.util.ApiUtil;
import org.wikipedia.util.ReleaseUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.zero.WikipediaZeroHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import okhttp3.Headers;
import okhttp3.Request;

import static org.wikipedia.util.DimenUtil.getFontSizeFromSp;
import static org.wikipedia.util.ReleaseUtil.getChannel;
import static org.wikipedia.util.StringUtil.emptyIfNull;

public class WikipediaApp extends Application {
    private static final int EVENT_LOG_TESTING_ID = new Random().nextInt(Integer.MAX_VALUE);

    public static final int FONT_SIZE_MULTIPLIER_MIN = -5;
    public static final int FONT_SIZE_MULTIPLIER_MAX = 8;
    private static final float FONT_SIZE_FACTOR = 0.1f;

    private final RemoteConfig remoteConfig = new RemoteConfig();
    private final UserInfoStorage userInfoStorage = new UserInfoStorage();
    private final Map<Class<?>, DatabaseClient<?>> databaseClients = Collections.synchronizedMap(new HashMap<Class<?>, DatabaseClient<?>>());
    private final Map<String, Api> apis = new HashMap<>();
    private AppLanguageState appLanguageState;
    private FunnelManager funnelManager;
    private SessionFunnel sessionFunnel;
    private ContentObserver readingListPageObserver;

    private Database database;
    private EditTokenStorage editTokenStorage;
    private SharedPreferenceCookieManager cookieManager;
    private String userAgent;
    private Site site;

    private CrashReporter crashReporter;

    public boolean isProdRelease() {
        return ReleaseUtil.isProdRelease();
    }

    public boolean isPreProdRelease() {
        return ReleaseUtil.isPreProdRelease();
    }

    public boolean isAlphaRelease() {
        return ReleaseUtil.isAlphaRelease();
    }

    public boolean isPreBetaRelease() {
        return ReleaseUtil.isPreBetaRelease();
    }

    public boolean isDevRelease() {
        return ReleaseUtil.isDevRelease();
    }

    public SessionFunnel getSessionFunnel() {
        return sessionFunnel;
    }

    /**
     * Singleton instance of WikipediaApp
     */
    private static WikipediaApp INSTANCE;

    private Bus bus;
    @NonNull
    private Theme currentTheme = Theme.getFallback();

    private WikipediaZeroHandler zeroHandler;
    public WikipediaZeroHandler getWikipediaZeroHandler() {
        return zeroHandler;
    }

    /**
     * Our page cache, which discards the eldest entries based on access time.
     * This will allow the user to go "back" smoothly (the previous page is guaranteed
     * to be in cache), but also to go "forward" smoothly (if the user clicks on a link
     * that was already visited within a short time).
     */
    private PageCache pageCache;
    public PageCache getPageCache() {
        return pageCache;
    }

    public WikipediaApp() {
        INSTANCE = this;
    }

    /**
     * Returns the singleton instance of the WikipediaApp
     *
     * This is ok, since android treats it as a singleton anyway.
     */
    public static WikipediaApp getInstance() {
        return INSTANCE;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initExceptionHandling();

        // See Javadocs and http://developer.android.com/tools/support-library/index.html#rev23-4-0
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        bus = new Bus();

        ViewAnimations.init(getResources());
        currentTheme = unmarshalCurrentTheme();

        appLanguageState = new AppLanguageState(this);
        funnelManager = new FunnelManager(this);
        sessionFunnel = new SessionFunnel(this);
        editTokenStorage = new EditTokenStorage(this);
        cookieManager = new SharedPreferenceCookieManager();
        database = new Database(this);

        enableWebViewDebugging();

        OkHttpConnectionFactory okHttpConnectionFactory = new OkHttpConnectionFactory(this);
        Api.setConnectionFactory(okHttpConnectionFactory);

        ImagePipelineConfig config = OkHttpImagePipelineConfigFactory
                .newBuilder(this, okHttpConnectionFactory.client())
                .build();
        Fresco.initialize(this, config);

        zeroHandler = new WikipediaZeroHandler(this);
        pageCache = new PageCache(this);

        // TODO: remove this code after all logged in users also have a system account or August 2016.
        AccountUtil.createAccountForLoggedInUser();

        UserOptionContentResolver.registerAppSyncObserver(this);
        registerReadingListPageObserver();
    }

    public Bus getBus() {
        return bus;
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
    public String getAcceptLanguage(@Nullable Site site) {
        String siteLang = site == null || "meta".equals(site.languageCode())
                ? ""
                : emptyIfNull(site.languageCode());
        return AcceptLanguageUtil.getAcceptLanguage(siteLang, emptyIfNull(getAppLanguageCode()),
                appLanguageState.getSystemLanguageCode());
    }

    public Api getAPIForSite(Site site) {
        return getAPIForSite(site, false);
    }

    public Api getAPIForSite(Site site, boolean mobile) {
        String host = mobile ? site.mobileHost() : site.host();
        String acceptLanguage = getAcceptLanguage(site);
        Map<String, String> customHeaders = buildCustomHeadersMap(acceptLanguage);
        Api api;

        String cachedApiKey = host + "-" + acceptLanguage;
        if (apis.containsKey(cachedApiKey)) {
            api = apis.get(cachedApiKey);
        } else {
            api = new Api(host, site.port(), site.secureScheme(),
                    site.path("api.php"), customHeaders);
            apis.put(cachedApiKey, api);
        }

        api.setHeaderCheckListener(zeroHandler);
        return api;
    }

    public Api getApiForMobileSite(Site site) {
        return getAPIForSite(site, true);
    }

    /**
     * Default site for the app
     * You should use PageTitle.getSite() to get the article site
     */
    @NonNull public Site getSite() {
        // TODO: why don't we ensure that the app language hasn't changed here instead of the client?
        if (site == null) {
            String lang = Prefs.getMediaWikiBaseUriSupportsLangCode() ? getAppOrSystemLanguageCode() : "";
            site = Site.forLanguageCode(lang);
        }
        return site;
    }

    /**
     * Convenience method to get an API object for the app site.
     *
     * @return An API object that is equivalent to calling getAPIForSite(getSite)
     */
    public Api getSiteApi() {
        return getAPIForSite(getSite());
    }

    @Nullable
    public String getAppLanguageCode() {
        return appLanguageState.getAppLanguageCode();
    }

    @NonNull
    public String getAppOrSystemLanguageCode() {
        return appLanguageState.getAppOrSystemLanguageCode();
    }

    @NonNull
    public String getSystemLanguageCode() {
        return appLanguageState.getSystemLanguageCode();
    }

    public void setAppLanguageCode(@Nullable String code) {
        appLanguageState.setAppLanguageCode(code);
        resetSite();
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

    @NonNull
    public ContentObserver getReadingListPageObserver() {
        return readingListPageObserver;
    }

    public Database getDatabase() {
        return database;
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
            } else if (cls.equals(SavedPage.class)) {
                client = new DatabaseClient<>(this, SavedPage.DATABASE_TABLE);
            } else if (cls.equals(EditSummary.class)) {
                client = new DatabaseClient<>(this, EditSummary.DATABASE_TABLE);
            } else if (cls.equals(UserOption.class)) {
                client = new DatabaseClient<>(this, UserOptionRow.DATABASE_TABLE);
            } else if (cls.equals(UserOptionRow.class)) {
                client = new DatabaseClient<>(this, UserOptionRow.HTTP_DATABASE_TABLE);
            } else if (cls.equals(ReadingListPageRow.class)) {
                client = new DatabaseClient<>(this, ReadingListPageRow.DATABASE_TABLE);
            } else if (cls.equals(ReadingListPageHttpRow.class)) {
                client = new DatabaseClient<>(this, ReadingListPageRow.HTTP_DATABASE_TABLE);
            } else if (cls.equals(ReadingListPageDiskRow.class)) {
                client = new DatabaseClient<>(this, ReadingListPageRow.DISK_DATABASE_TABLE);
            } else if (cls.equals(ReadingListRow.class)) {
                client = new DatabaseClient<>(this, ReadingListRow.DATABASE_TABLE);
            } else {
                throw new RuntimeException("No persister found for class " + cls.getCanonicalName());
            }
            databaseClients.put(cls, client);
        }
        //noinspection unchecked
        return (DatabaseClient<T>) databaseClients.get(cls);
    }

    public RemoteConfig getRemoteConfig() {
        return remoteConfig;
    }

    public EditTokenStorage getEditTokenStorage() {
        return editTokenStorage;
    }

    public SharedPreferenceCookieManager getCookieManager() {
        return cookieManager;
    }

    public UserInfoStorage getUserInfoStorage() {
        return userInfoStorage;
    }

    public void logOut() {
        L.v("logging out");
        AccountUtil.removeAccount();
        UserOptionDao.instance().clear();
        getEditTokenStorage().clearAllTokens();
        getCookieManager().clearAllCookies();
        getUserInfoStorage().clearUser();
    }

    public FunnelManager getFunnelManager() {
        return funnelManager;
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
     * Get an integer-valued random ID. This is typically used to determine global EventLogging
     * sampling, that is, whether the user's instance of the app sends any events or not. This is a
     * pure technical measure which is necessary to prevent overloading EventLogging with too many
     * events. This value will persist for the lifetime of the app.
     *
     * Don't use this method when running to determine whether or not the user falls into a control
     * or test group in any kind of tests (such as A/B tests), as that would introduce sampling
     * biases which would invalidate the test.
     * @return Integer ID for event log sampling.
     */
    @IntRange(from = 0)
    public int getEventLogSamplingID() {
        return EVENT_LOG_TESTING_ID;
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

    public boolean isCurrentThemeLight() {
        return getCurrentTheme().isLight();
    }

    public boolean isCurrentThemeDark() {
        return getCurrentTheme().isDark();
    }

    /**
     * Sets the theme of the app. If the new theme is the same as the current theme, nothing happens.
     * Otherwise, an event is sent to notify of the theme change.
     */
    public void setCurrentTheme(@NonNull Theme theme) {
        if (theme != currentTheme) {
            currentTheme = theme;
            Prefs.setThemeId(currentTheme.getMarshallingId());
            UserOptionDao.instance().theme(theme);
            bus.post(new ThemeChangeEvent());
        }
    }

    public int getFontSizeMultiplier() {
        return Prefs.getTextSizeMultiplier();
    }

    public void setFontSizeMultiplier(int multiplier) {
        if (multiplier < FONT_SIZE_MULTIPLIER_MIN) {
            multiplier = FONT_SIZE_MULTIPLIER_MIN;
        } else if (multiplier > FONT_SIZE_MULTIPLIER_MAX) {
            multiplier = FONT_SIZE_MULTIPLIER_MAX;
        }
        if (multiplier != Prefs.getTextSizeMultiplier()) {
            Prefs.setTextSizeMultiplier(multiplier);
            UserOptionDao.instance().fontSize(multiplier);
            bus.post(new ChangeTextSizeEvent());
        }
    }

    public void putCrashReportProperty(String key, String value) {
        crashReporter.putReportProperty(key, value);
    }

    public void checkCrashes(@NonNull Activity activity) {
        crashReporter.checkCrashes(activity);
    }

    public void runOnMainThread(Runnable runnable) {
        new Handler(getMainLooper()).post(runnable);
    }

    /**
     * Gets the current size of the app's font. This is given as a device-specific size (not "sp"),
     * and can be passed directly to setTextSize() functions.
     * @param window The window on which the font will be displayed.
     * @return Actual current size of the font.
     */
    public float getFontSize(Window window) {
        return getFontSizeFromSp(window,
                getResources().getDimension(R.dimen.textSize)) * (1.0f + getFontSizeMultiplier() * FONT_SIZE_FACTOR);
    }

    /**
     * Gets whether EventLogging is currently enabled or disabled.
     *
     * @return A boolean that is true if EventLogging is enabled, and false if it is not.
     */
    public boolean isEventLoggingEnabled() {
        return Prefs.isEventLoggingEnabled();
    }

    public boolean isImageDownloadEnabled() {
        return Prefs.isImageDownloadEnabled();
    }

    public boolean isLinkPreviewEnabled() {
        return Prefs.isLinkPreviewEnabled();
    }

    public void resetSite() {
        site = null;
    }

    public OnboardingStateMachine getOnboardingStateMachine() {
        return PrefsOnboardingStateMachine.getInstance();
    }

    /** For Retrofit requests. Keep in sync with #buildCustomHeadersMap */
    public Headers buildCustomHeaders(Request request, Site site) {
        Map<String, String> toSetHeaders = buildCustomHeadersMap(getAcceptLanguage(site));

        Headers.Builder moreHeaders = request.headers().newBuilder();
        for (String key : toSetHeaders.keySet()) {
            moreHeaders.set(key, toSetHeaders.get(key));
        }
        return moreHeaders.build();
    }

    /** For java-mwapi API requests. */
    private Map<String, String> buildCustomHeadersMap(String acceptLanguage) {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", getUserAgent());

        if (isEventLoggingEnabled()) {
            headers.put("X-WMF-UUID", getAppInstallID());
        } else {
            // Send do-not-track header if the user has opted out of event logging
            headers.put("DNT", "1");
        }

        headers.put("Accept-Language", acceptLanguage);
        return headers;
    }

    private void initExceptionHandling() {
        crashReporter = new HockeyAppCrashReporter(getString(R.string.hockeyapp_app_id), consentAccessor());
        crashReporter.registerCrashHandler(this);

        L.setRemoteLogger(crashReporter);
    }

    private CrashReporter.AutoUploadConsentAccessor consentAccessor() {
        return new CrashReporter.AutoUploadConsentAccessor() {
            @Override
            public boolean isAutoUploadPermitted() {
                return Prefs.isCrashReportAutoUploadEnabled();
            }
        };
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void enableWebViewDebugging() {
        if (BuildConfig.DEBUG && ApiUtil.hasKitKat()) {
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

    private void registerReadingListPageObserver() {
        readingListPageObserver = new ReadingListPageObserver(null);
        Uri readingListPageBaseUri = ReadingListPageContract.Disk.URI;
        Uri uriWithQuery = readingListPageBaseUri.buildUpon()
                .appendQueryParameter(AppContentProviderContract.NOTIFY, "false").build();
        WikipediaApp.getInstance().getContentResolver()
                .registerContentObserver(uriWithQuery, true, readingListPageObserver);
        L.i("Registered reading list page observer");
    }
}
