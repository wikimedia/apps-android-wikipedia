package org.wikipedia;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Window;
import android.webkit.WebView;

import com.squareup.otto.Bus;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.mediawiki.api.json.Api;
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
import org.wikipedia.util.log.L;
import org.wikipedia.zero.WikipediaZeroHandler;

import retrofit.RequestInterceptor;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;

import static org.wikipedia.util.StringUtil.emptyIfNull;

@ReportsCrashes(
        formKey = "",
        mode = ReportingInteractionMode.DIALOG,
        resDialogTitle = R.string.acra_report_dialog_title,
        resDialogText = R.string.acra_report_dialog_text,
        resDialogCommentPrompt = R.string.acra_report_dialog_comment,
        mailTo = "mobile-android-wikipedia-crashes@wikimedia.org")
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

    private boolean sslFallback = false;
    public boolean getSslFallback() {
        return sslFallback;
    }
    public void setSslFallback(boolean fallback) {
        sslFallback = fallback;
    }

    private int sslFailCount = 0;
    public int getSslFailCount() {
        return sslFailCount;
    }
    public int incSslFailCount() {
        return ++sslFailCount;
    }

    public static final int FONT_SIZE_MULTIPLIER_MIN = -5;
    public static final int FONT_SIZE_MULTIPLIER_MAX = 8;
    private static final float FONT_SIZE_FACTOR = 0.1f;

    public static final int RELEASE_PROD = 0;
    public static final int RELEASE_BETA = 1;
    public static final int RELEASE_ALPHA = 2;
    public static final int RELEASE_DEV = 3;
    private final int releaseType;

    public static final int PREFERRED_THUMB_SIZE = 320;

    private AppLanguageState appLanguageState;

    /**
     * Returns a constant that tells whether this app is a production release,
     * a beta release, or an alpha (continuous integration) release.
     * @return Release type of the app.
     */
    public int getReleaseType() {
        return releaseType;
    }

    public boolean isProdRelease() {
        return releaseType == RELEASE_PROD;
    }

    public boolean isDevRelease() {
        return releaseType == RELEASE_DEV;
    }

    public boolean isPreBetaRelease() {
        switch (getReleaseType()) {
            case RELEASE_PROD:
            case RELEASE_BETA:
                return false;
            default:
                return true;
        }
    }

    private SessionFunnel sessionFunnel;
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

        releaseType = calculateReleaseType();
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
        ACRA.init(this);


        bus = new Bus();

        final Resources resources = getResources();
        ViewAnimations.init(resources);
        screenDensity = resources.getDisplayMetrics().density;
        currentTheme = unmarshalCurrentTheme();

        appLanguageState = new AppLanguageState(this);

        sessionFunnel = new SessionFunnel(this);

        enableWebViewDebugging();

        Api.setConnectionFactory(new OkHttpConnectionFactory(this));

        zeroHandler = new WikipediaZeroHandler(this);
        pageCache = new PageCache(this);
    }

    public Bus getBus() {
        return bus;
    }

    private String userAgent;
    public String getUserAgent() {
        if (userAgent == null) {
            String channel = Utils.getChannel(this);
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

    private HashMap<String, Api> apis = new HashMap<>();
    private MccMncStateHandler mccMncStateHandler = new MccMncStateHandler();
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
            String domainAndApiAndVariantKey = site.getDomain() + "-" + site.getApiDomain()
                    + "-" + acceptLanguage + "-" + isEventLoggingEnabled();
            if (apis.containsKey(domainAndApiAndVariantKey)) {
                api = apis.get(domainAndApiAndVariantKey);
            } else {
                api = new Api(site.getApiDomain(), site.getUseSecure(),
                        site.getScriptPath("api.php"), customHeaders);
                apis.put(domainAndApiAndVariantKey, api);
            }
        }

        api.setHeaderCheckListener(zeroHandler);
        return api;
    }

    private Site primarySite;

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

    private DBOpenHelper dbOpenHelper;
    public DBOpenHelper getDbOpenHelper() {
        if (dbOpenHelper == null) {
            dbOpenHelper = new DBOpenHelper(this);
        }
        return dbOpenHelper;
    }

    private HashMap<String, ContentPersister> persisters = new HashMap<>();
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


    private RemoteConfig remoteConfig;
    public RemoteConfig getRemoteConfig() {
        if (remoteConfig == null) {
            remoteConfig = new RemoteConfig();
        }
        return remoteConfig;
    }

    private EditTokenStorage editTokenStorage;
    public EditTokenStorage getEditTokenStorage() {
        if (editTokenStorage == null) {
            editTokenStorage = new EditTokenStorage(this);
        }
        return editTokenStorage;
    }

    private SharedPreferenceCookieManager cookieManager;
    public SharedPreferenceCookieManager getCookieManager() {
        if (cookieManager == null) {
            cookieManager = new SharedPreferenceCookieManager();
        }
        return cookieManager;
    }

    private UserInfoStorage userInfoStorage;
    public UserInfoStorage getUserInfoStorage() {
        if (userInfoStorage == null) {
            userInfoStorage = new UserInfoStorage();
        }
        return userInfoStorage;
    }

    private FunnelManager funnelManager;
    public FunnelManager getFunnelManager() {
        if (funnelManager == null) {
            funnelManager = new FunnelManager(this);
        }

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
        setDrawableTint(d, getColor(isCurrentThemeDark() ? R.color.button_dark : R.color.button_light));
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

    /**
     * Gets the current size of the app's font. This is given as a device-specific size (not "sp"),
     * and can be passed directly to setTextSize() functions.
     * @param window The window on which the font will be displayed.
     * @return Actual current size of the font.
     */
    public float getFontSize(Window window) {
        return Utils.getFontSizeFromSp(window,
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

    /** For Retrofit requests. Keep in sync with #buildCustomHeaders */
    public void injectCustomHeaders(RequestInterceptor.RequestFacade request, Site site) {
        request.addHeader("User-Agent", getUserAgent());

        // Add the app install ID to the header, but only if the user has not opted out of logging
        if (isEventLoggingEnabled()) {
            request.addHeader("X-WMF-UUID", getAppInstallID());
        }

        request.addHeader("Accept-Language", getAcceptLanguage(site));
    }

    /** For java-mwapi API requests. */
    private HashMap<String, String> buildCustomHeaders(String acceptLanguage) {
        // https://lists.wikimedia.org/pipermail/wikimedia-l/2014-April/071131.html
        HashMap<String, String> headers = new HashMap<>();

        headers.put("User-Agent", getUserAgent());

        // Add the app install ID to the header, but only if the user has not opted out of logging
        if (isEventLoggingEnabled()) {
            headers.put("X-WMF-UUID", getAppInstallID());
        }

        headers.put("Accept-Language", acceptLanguage);

        return headers;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void enableWebViewDebugging() {
        if (ApiUtil.hasKitKat()) {
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
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

    private int calculateReleaseType() {
        if (BuildConfig.APPLICATION_ID.contains("beta")) {
            return RELEASE_BETA;
        }
        if (BuildConfig.APPLICATION_ID.contains("alpha")) {
            return RELEASE_ALPHA;
        }
        if (BuildConfig.APPLICATION_ID.contains("dev")) {
            return RELEASE_DEV;
        }
        return RELEASE_PROD;
    }

    private int getColor(int id) {
        return getResources().getColor(id);
    }
}
