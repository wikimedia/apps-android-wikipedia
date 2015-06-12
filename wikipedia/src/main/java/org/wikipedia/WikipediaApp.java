package org.wikipedia;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
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
import org.wikipedia.editing.EditTokenStorage;
import org.wikipedia.editing.summaries.EditSummary;
import org.wikipedia.editing.summaries.EditSummaryPersister;
import org.wikipedia.events.ChangeTextSizeEvent;
import org.wikipedia.events.ThemeChangeEvent;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.history.HistoryEntryPersister;
import org.wikipedia.interlanguage.AppLanguageState;
import org.wikipedia.login.UserInfoStorage;
import org.wikipedia.migration.PerformMigrationsTask;
import org.wikipedia.networking.MccMncStateHandler;
import org.wikipedia.page.PageCache;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.pageimages.PageImagePersister;
import org.wikipedia.savedpages.SavedPage;
import org.wikipedia.savedpages.SavedPagePersister;
import org.wikipedia.search.RecentSearch;
import org.wikipedia.search.RecentSearchPersister;
import org.wikipedia.settings.PrefKeys;
import org.wikipedia.util.ApiUtil;
import org.wikipedia.zero.WikipediaZeroHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

@ReportsCrashes(
        formKey = "",
        mode = ReportingInteractionMode.DIALOG,
        resDialogTitle = R.string.acra_report_dialog_title,
        resDialogText = R.string.acra_report_dialog_text,
        resDialogCommentPrompt = R.string.acra_report_dialog_comment,
        mailTo = "mobile-android-wikipedia-crashes@wikimedia.org")
public class WikipediaApp extends Application {
    private static final String HTTPS_PROTOCOL = "https";

    private float screenDensity;
    public float getScreenDensity() {
        return screenDensity;
    }

    // Reload in onCreate to override
    private String networkProtocol = HTTPS_PROTOCOL;
    public String getNetworkProtocol() {
        return networkProtocol;
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

    public static final int THEME_LIGHT = R.style.Theme_WikiLight;
    public static final int THEME_DARK = R.style.Theme_WikiDark;

    public static final int FONT_SIZE_MULTIPLIER_MIN = -5;
    public static final int FONT_SIZE_MULTIPLIER_MAX = 8;
    private static final float FONT_SIZE_FACTOR = 0.1f;

    public static final int RELEASE_PROD = 0;
    public static final int RELEASE_BETA = 1;
    public static final int RELEASE_ALPHA = 2;
    public static final int RELEASE_DEV = 3;
    private int releaseType = RELEASE_PROD;

    public static final int PREFERRED_THUMB_SIZE = 96;

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


    private SessionFunnel sessionFunnel;
    public SessionFunnel getSessionFunnel() {
        return sessionFunnel;
    }

    /**
     * Singleton instance of WikipediaApp
     */
    private static WikipediaApp INSTANCE;

    private Bus bus;
    private int currentTheme = 0;

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

    /**
     * Preference manager for storing things like the app's install IDs for EventLogging, theme,
     * font size, etc.
     */
    private SharedPreferences prefs;

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
        ACRA.init(this);

        if (BuildConfig.APPLICATION_ID.contains("beta")) {
            releaseType = RELEASE_BETA;
        } else if (BuildConfig.APPLICATION_ID.contains("alpha")) {
            releaseType = RELEASE_ALPHA;
        } else if (BuildConfig.APPLICATION_ID.contains("dev")) {
            releaseType = RELEASE_DEV;
        }

        bus = new Bus();

        final Resources resources = getResources();
        ViewAnimations.init(resources);
        screenDensity = resources.getDisplayMetrics().density;

        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
        PrefKeys.initPreferenceKeys(resources);

        appLanguageState = new AppLanguageState(this);

        sessionFunnel = new SessionFunnel(this);

        // Enable debugging on the webview
        if (ApiUtil.hasKitKat()) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        Api.setConnectionFactory(new OkHttpConnectionFactory(this));

        zeroHandler = new WikipediaZeroHandler(this);
        pageCache = new PageCache(this);

        new PerformMigrationsTask().execute();
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

    @NonNull
    public String getAcceptLanguage() {
        return getAcceptLanguage(null);
    }

    /**
     * @return the value that should go in the Accept-Language header.
     */
    @NonNull
    public String getAcceptLanguage(@Nullable Site site) {
        /*Nonnull*/ String siteLanguageCode = site == null ? "" : site.getLanguageCode();
        /*Nonnull*/ String appLanguageCode = isSystemLanguageEnabled() ? "" : getAppLanguageCode();
        /*Nonnull*/ String systemLanguageCode = getSystemLanguageCode();
        final float appLanguageQuality = .9f;
        final float systemLanguageQuality = .8f;

        String acceptLanguage = siteLanguageCode;
        acceptLanguage = appendToAcceptLanguage(acceptLanguage, appLanguageCode, appLanguageQuality);
        acceptLanguage = appendToAcceptLanguage(acceptLanguage, systemLanguageCode, systemLanguageQuality);

        return acceptLanguage;
    }

    private HashMap<String, Api> apis = new HashMap<>();
    private MccMncStateHandler mccMncStateHandler = new MccMncStateHandler();
    public Api getAPIForSite(Site site) {
        // https://lists.wikimedia.org/pipermail/wikimedia-l/2014-April/071131.html
        HashMap<String, String> customHeaders = new HashMap<>();
        customHeaders.put("User-Agent", getUserAgent());
        // Add the app install ID to the header, but only if the user has not opted out of logging
        if (isEventLoggingEnabled()) {
            customHeaders.put("X-WMF-UUID", getAppInstallID());
        }
        String acceptLanguage = getAcceptLanguage(site);

        customHeaders.put("Accept-Language", acceptLanguage);

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

    public String getAppLanguageCode() {
        return appLanguageState.getAppLanguageCode();
    }

    public String getSystemLanguageCode() {
        return appLanguageState.getSystemLanguageCode();
    }

    public String getAppOrSystemLanguageCode() {
        return appLanguageState.getAppOrSystemLanguageCode();
    }

    public void setAppLanguageCode(String code) {
        appLanguageState.setAppLanguageCode(code);
        resetSite();
    }

    public boolean isSystemLanguageEnabled() {
        return appLanguageState.isSystemLanguageEnabled();
    }

    public void setSystemLanguageEnabled() {
        appLanguageState.setSystemLanguageEnabled();
    }

    public String getAppLanguageLocalizedName() {
        return appLanguageState.getAppLanguageLocalizedName();
    }

    public Locale getAppLocale() {
        return appLanguageState.getAppLocale();
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
            remoteConfig = new RemoteConfig(prefs);
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
            cookieManager = new SharedPreferenceCookieManager(prefs);
        }
        return cookieManager;
    }

    private UserInfoStorage userInfoStorage;
    public UserInfoStorage getUserInfoStorage() {
        if (userInfoStorage == null) {
            userInfoStorage = new UserInfoStorage(prefs);
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
        return getAppInstallIDForFeature(PrefKeys.getAppInstallID());
    }

    /**
     * Get an integer-valued random ID for A/B testing of new features. This value will persist
     * for the install lifetime of the app.
     * @return Integer ID for A/B testing.
     */
    public int getABTestingID() {
        return getFeatureFlagID();
    }

    /**
     * Get an integer-valued random ID for event log sampling. This value will persist for the
     * install lifetime of the app.
     * @return Integer ID for event log sampling.
     */
    public int getEventLogSamplingID() {
        return getFeatureFlagID();
    }

    @IntRange(from = 0)
    private int getFeatureFlagID() {
        int featureFlagID;
        if (prefs.contains(PrefKeys.getFeatureFlagID())) {
            featureFlagID = prefs.getInt(PrefKeys.getFeatureFlagID(), 0);
        } else {
            // generate a random number in the range [0, max-int)
            featureFlagID = new Random().nextInt(Integer.MAX_VALUE);
            prefs.edit().putInt(PrefKeys.getFeatureFlagID(), featureFlagID).apply();
        }
        // make sure the number is positive by taking away the sign bit
        // (will only apply to previously-generated values that happened to be negative)
        return featureFlagID & Integer.MAX_VALUE;
    }

    /**
     * Returns the unique app install ID for a feature. The app install ID is used to track unique
     * users of each feature for the purpose of improving the app's user experience.
     * @param  prefKey a key from PrefKeys for a feature with a unique app install ID
     * @return         the unique app install ID of the specified feature
     */
    private String getAppInstallIDForFeature(String prefKey) {
        String installID;
        if (prefs.contains(prefKey)) {
            installID = prefs.getString(prefKey, null);
        } else {
            installID = UUID.randomUUID().toString();
            prefs.edit().putString(prefKey, installID).apply();
        }
        return installID;
    }

    /**
     * Gets the currently-selected theme for the app.
     * @return Theme that is currently selected, which is the actual theme ID that can
     * be passed to setTheme() when creating an activity.
     */
    public int getCurrentTheme() {
        if (currentTheme == 0) {
            currentTheme = prefs.getInt(PrefKeys.getColorTheme(), THEME_LIGHT);
            if (currentTheme != THEME_LIGHT && currentTheme != THEME_DARK) {
                currentTheme = THEME_LIGHT;
            }
        }
        return currentTheme;
    }

    /**
     * Sets the theme of the app. If the new theme is the same as the current theme, nothing happens.
     * Otherwise, an event is sent to notify of the theme change.
     * @param newTheme
     */
    public void setCurrentTheme(int newTheme) {
        if (newTheme == currentTheme) {
            return;
        }
        currentTheme = newTheme;
        prefs.edit().putInt(PrefKeys.getColorTheme(), currentTheme).apply();
        bus.post(new ThemeChangeEvent());
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
            d.setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP);
        }
    }

    /**
     * Make adjustments to a Drawable object to look better in the current theme.
     * (e.g. apply a white color filter for night mode)
     * @param d Drawable to be adjusted.
     */
    public void adjustDrawableToTheme(Drawable d) {
        setDrawableTint(d, currentTheme == THEME_DARK ? Color.WHITE : 0);
    }

    /**
     * Make adjustments to a link or button Drawable object to look better in the current theme.
     * (e.g. apply a blue color filter for night mode, )
     * @param d Drawable to be adjusted.
     */
    public void adjustLinkDrawableToTheme(Drawable d) {
        setDrawableTint(d, getResources().getColor(currentTheme == THEME_DARK ? R.color.button_dark : R.color.button_light));
    }

    public int getFontSizeMultiplier() {
        return prefs.getInt(PrefKeys.getTextSizeMultiplier(), 0);
    }

    public void setFontSizeMultiplier(int multiplier) {
        if (multiplier < FONT_SIZE_MULTIPLIER_MIN) {
            multiplier = FONT_SIZE_MULTIPLIER_MIN;
        } else if (multiplier > FONT_SIZE_MULTIPLIER_MAX) {
            multiplier = FONT_SIZE_MULTIPLIER_MAX;
        }
        prefs.edit().putInt(PrefKeys.getTextSizeMultiplier(), multiplier).apply();
        bus.post(new ChangeTextSizeEvent());
    }

    /**
     * Gets the current size of the app's font. This is given as a device-specific size (not "sp"),
     * and can be passed directly to setTextSize() functions.
     * @param window The window on which the font will be displayed.
     * @return Actual current size of the font.
     */
    public float getFontSize(Window window) {
        int multiplier = prefs.getInt(PrefKeys.getTextSizeMultiplier(), 0);
        return Utils.getFontSizeFromSp(window, getResources().getDimension(R.dimen.textSize)) * (1.0f + multiplier * FONT_SIZE_FACTOR);
    }

    /**
     * Gets whether EventLogging is currently enabled or disabled.
     *
     * @return A boolean that is true if EventLogging is enabled, and false if it is not.
     */
    public boolean isEventLoggingEnabled() {
        return prefs.getBoolean(PrefKeys.getEventLoggingEnabled(), true);
    }

    public boolean showImages() {
        return prefs.getBoolean(PrefKeys.getShowImages(), true);
    }

    public void resetSite() {
        primarySite = null;
    }

    @NonNull private String appendToAcceptLanguage(@NonNull String acceptLanguage,
            @NonNull String languageCode, float quality) {
        // If accept-language already contains the language, just return accept-language.
        if (acceptLanguage.contains(languageCode)) {
            return acceptLanguage;
        }

        // If accept-language is empty, don't append. Just return the language.
        if (acceptLanguage.isEmpty()) {
            return languageCode;
        }

        // Accept-language is nonempty, append the language.
        return String.format("%s,%s;q=%.1f", acceptLanguage, languageCode, quality);
    }
}
