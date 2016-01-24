package org.wikipedia;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Window;
import android.webkit.WebView;

import com.squareup.otto.Bus;

import org.mediawiki.api.json.Api;
import org.wikipedia.crash.CrashReporter;
import org.wikipedia.crash.hockeyapp.HockeyAppCrashReporter;
import org.wikipedia.analytics.FunnelManager;
import org.wikipedia.analytics.SessionFunnel;
import org.wikipedia.data.ContentPersister;
import org.wikipedia.data.DBOpenHelper;
import org.wikipedia.drawable.DrawableUtil;
import org.wikipedia.editing.EditTokenStorage;
import org.wikipedia.editing.summaries.EditSummary;
import org.wikipedia.editing.summaries.EditSummaryPersister;
import org.wikipedia.events.ChangeTextSizeEvent;
import org.wikipedia.events.ThemeChangeEvent;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.history.HistoryEntryPersister;
import org.wikipedia.interlanguage.AcceptLanguageUtil;
import org.wikipedia.interlanguage.AppLanguageState;
import org.wikipedia.login.UserInfoStorage;
import org.wikipedia.networking.MccMncStateHandler;
import org.wikipedia.onboarding.OnboardingStateMachine;
import org.wikipedia.onboarding.PrefsOnboardingStateMachine;
import org.wikipedia.page.PageCache;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.pageimages.PageImagePersister;
import org.wikipedia.savedpages.SavedPage;
import org.wikipedia.savedpages.SavedPagePersister;
import org.wikipedia.search.RecentSearch;
import org.wikipedia.search.RecentSearchPersister;
import org.wikipedia.settings.Prefs;
import org.wikipedia.theme.Theme;
import org.wikipedia.util.ApiUtil;
import org.wikipedia.util.ReleaseUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.zero.WikipediaZeroHandler;

import retrofit.RequestInterceptor;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;

import static org.wikipedia.util.DimenUtil.getFontSizeFromSp;
import static org.wikipedia.util.StringUtil.emptyIfNull;
import static org.wikipedia.util.ReleaseUtil.getChannel;

public class WikipediaApp extends Application {
    private static final String HTTPS_PROTOCOL = "https";
    private static final int EVENT_LOG_TESTING_ID = new Random().nextInt(Integer.MAX_VALUE);

    private float screenDensity;
    public float getScreenDensity() {
        return screenDensity;
    }

    // Reload in onCreate to override
    public String getNetworkProtocol() {
        return HTTPS_PROTOCOL;
    }

    public static final int FONT_SIZE_MULTIPLIER_MIN = -5;
    public static final int FONT_SIZE_MULTIPLIER_MAX = 8;
    private static final float FONT_SIZE_FACTOR = 0.1f;

    public static final int PREFERRED_THUMB_SIZE = 320;

    private final RemoteConfig remoteConfig = new RemoteConfig();
    private final UserInfoStorage userInfoStorage = new UserInfoStorage();
    private final MccMncStateHandler mccMncStateHandler = new MccMncStateHandler();
    private final Map<String, ContentPersister> persisters = Collections.synchronizedMap(new HashMap<String, ContentPersister>());
    private final HashMap<String, Api> apis = new HashMap<>();
    private AppLanguageState appLanguageState;
    private FunnelManager funnelManager;
    private SessionFunnel sessionFunnel;

    private DBOpenHelper dbOpenHelper;
    private EditTokenStorage editTokenStorage;
    private SharedPreferenceCookieManager cookieManager;
    private String userAgent;
    private Site primarySite;

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

        bus = new Bus();

        final Resources resources = getResources();
        ViewAnimations.init(resources);
        screenDensity = resources.getDisplayMetrics().density;
        currentTheme = unmarshalCurrentTheme();

        appLanguageState = new AppLanguageState(this);
        funnelManager = new FunnelManager(this);
        sessionFunnel = new SessionFunnel(this);
        editTokenStorage = new EditTokenStorage(this);
        cookieManager = new SharedPreferenceCookieManager();
        dbOpenHelper = new DBOpenHelper(this);

        enableWebViewDebugging();

        Api.setConnectionFactory(new OkHttpConnectionFactory(this));

        zeroHandler = new WikipediaZeroHandler(this);
        pageCache = new PageCache(this);
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
        return AcceptLanguageUtil.getAcceptLanguage(
                site == null ? "" : emptyIfNull(site.getLanguageCode()),
                emptyIfNull(getAppLanguageCode()), appLanguageState.getSystemLanguageCode());
    }

    public MccMncStateHandler getMccMncStateHandler() {
        return mccMncStateHandler;
    }
    public Api getAPIForSite(Site site) {
        String acceptLanguage = getAcceptLanguage(site);
        HashMap<String, String> customHeaders = buildCustomHeaders(acceptLanguage);

        // Because the mccMnc enrichment is a one-time thing, we don't need to have a complex hash key
        // for the apis HashMap<String, Api> like we do below. It naturally gets the correct
        // Accept-Language header from above, when applicable.
        Api api = mccMncStateHandler.makeApiWithMccMncHeaderEnrichment(this, site, customHeaders);
        if (api == null) {
            String domainAndApiAndVariantKey = site.getDomain() + "-" + site.getDomain()
                    + "-" + acceptLanguage + "-" + isEventLoggingEnabled();
            if (apis.containsKey(domainAndApiAndVariantKey)) {
                api = apis.get(domainAndApiAndVariantKey);
            } else {
                api = new Api(site.getDomain(), site.getUseSecure(),
                        site.getScriptPath("api.php"), customHeaders);
                apis.put(domainAndApiAndVariantKey, api);
            }
        }

        api.setHeaderCheckListener(zeroHandler);
        return api;
    }

    /**
     * Default site of the application
     * You should use PageTitle.getSite() to get the currently browsed site
     */
    public Site getPrimarySite() {
        if (primarySite == null) {
            primarySite = Site.forLanguage(getAppOrSystemLanguageCode());
        }

        return primarySite;
    }

    /**
     * Convenience method to get an API object for the primary site.
     *
     * @return An API object that is equivalent to calling getAPIForSite(getPrimarySite)
     */
    public Api getPrimarySiteApi() {
        return getAPIForSite(getPrimarySite());
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

    public DBOpenHelper getDbOpenHelper() {
        return dbOpenHelper;
    }

    public ContentPersister getPersister(Class cls) {
        if (!persisters.containsKey(cls.getCanonicalName())) {
            ContentPersister persister;
            if (cls.equals(HistoryEntry.class)) {
                persister = new HistoryEntryPersister(this);
            } else if (cls.equals(PageImage.class)) {
                persister = new PageImagePersister(this);
            } else if (cls.equals(RecentSearch.class)) {
                persister = new RecentSearchPersister(this);
            } else if (cls.equals(SavedPage.class)) {
                persister = new SavedPagePersister(this);
            } else if (cls.equals(EditSummary.class)) {
                persister = new EditSummaryPersister(this);
            } else {
                throw new RuntimeException("No persister found for class " + cls.getCanonicalName());
            }
            persisters.put(cls.getCanonicalName(), persister);
        }
        return persisters.get(cls.getCanonicalName());
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
     * Get an integer-valued random ID for event log sampling. This value will persist for the
     * lifetime of the app.
     * @return Integer ID for event log sampling.
     */
    @IntRange(from = 0)
    public int getEventLogSamplingID() {
        return EVENT_LOG_TESTING_ID;
    }

    public boolean isFeatureSelectTextAndShareTutorialEnabled() {
        boolean enabled;
        if (Prefs.hasFeatureSelectTextAndShareTutorial()) {
            enabled = Prefs.isFeatureSelectTextAndShareTutorialEnabled();
        } else {
            enabled = new Random().nextInt(2) == 0;
            Prefs.setFeatureSelectTextAndShareTutorialEnabled(enabled);
        }
        return enabled;
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

    @ColorInt
    public int getContrastingThemeColor() {
        return getCurrentTheme().getContrastingColor();
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

    /**
     * Apply a tint to the provided drawable.
     * @param d Drawable to be tinted.
     * @param tintColor Color of the tint. Setting to 0 will remove the tint.
     */
    public void setDrawableTint(Drawable d, int tintColor) {
        if (tintColor == 0) {
            d.clearColorFilter();
        } else {
            DrawableUtil.setTint(d, tintColor);
        }
    }

    /**
     * Make adjustments to a Drawable object to look better in the current theme.
     * (e.g. apply a white color filter for night mode)
     * @param d Drawable to be adjusted.
     */
    public void adjustDrawableToTheme(Drawable d) {
        setDrawableTint(d, isCurrentThemeDark() ? Color.WHITE : Color.TRANSPARENT);
    }

    /**
     * Make adjustments to a link or button Drawable object to look better in the current theme.
     * (e.g. apply a blue color filter for night mode, )
     * @param d Drawable to be adjusted.
     */
    public void adjustLinkDrawableToTheme(Drawable d) {
        setDrawableTint(d, getResources().getColor(isCurrentThemeDark() ? R.color.button_dark : R.color.button_light));
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
        Prefs.setTextSizeMultiplier(multiplier);
        bus.post(new ChangeTextSizeEvent());
    }

    public void putCrashReportProperty(String key, String value) {
        crashReporter.putReportProperty(key, value);
    }

    public void checkCrashes(@NonNull Activity activity) {
        crashReporter.checkCrashes(activity);
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
        primarySite = null;
    }

    public OnboardingStateMachine getOnboardingStateMachine() {
        return PrefsOnboardingStateMachine.getInstance();
    }

    public SimpleDateFormat getSimpleDateFormat() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return simpleDateFormat;
    }

    /** For Retrofit requests. */
    public void injectCustomHeaders(RequestInterceptor.RequestFacade request, Site site) {
        HashMap<String, String> headers = buildCustomHeaders(getAcceptLanguage(site));
        for (String key : headers.keySet()) {
            request.addHeader(key, headers.get(key));
        }
    }

    /** For java-mwapi API requests. */
    private HashMap<String, String> buildCustomHeaders(String acceptLanguage) {
        HashMap<String, String> headers = new HashMap<>();
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
}
