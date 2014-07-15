package org.wikipedia;

import android.app.Application;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Window;
import android.webkit.WebView;
import com.squareup.otto.Bus;
import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.mediawiki.api.json.Api;
import org.wikipedia.analytics.FunnelManager;
import org.wikipedia.bridge.StyleLoader;
import org.wikipedia.data.ContentPersister;
import org.wikipedia.data.DBOpenHelper;
import org.wikipedia.editing.EditTokenStorage;
import org.wikipedia.editing.summaries.EditSummary;
import org.wikipedia.editing.summaries.EditSummaryPersister;
import org.wikipedia.events.ChangeTextSizeEvent;
import org.wikipedia.events.ThemeChangeEvent;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.history.HistoryEntryPersister;
import org.wikipedia.login.UserInfoStorage;
import org.wikipedia.migration.PerformMigrationsTask;
import org.wikipedia.networking.ConnectionChangeReceiver;
import org.wikipedia.networking.MccMncStateHandler;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.pageimages.PageImagePersister;
import org.wikipedia.savedpages.SavedPage;
import org.wikipedia.savedpages.SavedPagePersister;

import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;


@ReportsCrashes(
        formKey = "",
        mode = ReportingInteractionMode.DIALOG,
        resDialogTitle = R.string.acra_report_dialog_title,
        resDialogText = R.string.acra_report_dialog_text,
        resDialogCommentPrompt = R.string.acra_report_dialog_comment,
        mailTo = "mobile-android-wikipedia-crashes@wikimedia.org")
public class WikipediaApp extends Application {
    private Bus bus;

    public static long SHORT_ANIMATION_DURATION;
    public static long MEDIUM_ANIMATION_DURATION;

    public static String PREFERENCE_CONTENT_LANGUAGE;
    public static String PREFERENCE_COOKIE_DOMAINS;
    public static String PREFERENCE_COOKIES_FOR_DOMAINS;
    public static String PREFERENCE_EDITTOKEN_WIKIS;
    public static String PREFERENCE_EDITTOKEN_FOR_WIKI;
    public static String PREFERENCE_ZERO_INTERSTITIAL;
    public static String PREFERENCE_ZERO_DEVMODE;
    public static String PREFERENCE_REMOTE_CONFIG;
    public static String PREFERENCE_EVENTLOGGING_ENABLED;
    public static String PREFERENCE_STYLES_LAST_UPDATED;
    public static String PREFERENCE_READING_APP_INSTALL_ID;
    public static String PREFERENCE_ONBOARD;
    public static String PREFERENCE_TEXT_SIZE_MULTIPLIER;
    public static String PREFERENCE_COLOR_THEME;

    public static float SCREEN_DENSITY;
    // Reload in onCreate to override
    public static String PROTOCOL = "https";

    public static String APP_VERSION_STRING;

    public static int THEME_LIGHT;
    public static int THEME_DARK;
    private int currentTheme = 0;

    public static final int FONT_SIZE_MULTIPLIER_MIN = -5;
    public static final int FONT_SIZE_MULTIPLIER_MAX = 8;

    /**
     * Singleton instance of WikipediaApp
     */
    private static WikipediaApp INSTANCE;

    private ConnectionChangeReceiver connChangeReceiver;
    public static boolean FALLBACK = false;
    public static int FAILS = 0;

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

        bus = new Bus();

        SHORT_ANIMATION_DURATION = getResources().getInteger(android.R.integer.config_shortAnimTime);
        MEDIUM_ANIMATION_DURATION = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        SCREEN_DENSITY = getResources().getDisplayMetrics().density;

        PREFERENCE_CONTENT_LANGUAGE = getResources().getString(R.string.preference_key_language);
        PREFERENCE_COOKIE_DOMAINS = getString(R.string.preference_cookie_domains);
        PREFERENCE_COOKIES_FOR_DOMAINS = getString(R.string.preference_cookies_for_domain);
        PREFERENCE_EDITTOKEN_WIKIS = getString(R.string.preference_edittoken_wikis);
        PREFERENCE_EDITTOKEN_FOR_WIKI = getString(R.string.preference_edittoken_for_wiki);
        PREFERENCE_ZERO_INTERSTITIAL = getResources().getString(R.string.preference_key_zero_interstitial);
        PREFERENCE_ZERO_DEVMODE = getResources().getString(R.string.preference_key_zero_devmode);
        PREFERENCE_REMOTE_CONFIG = getString(R.string.preference_key_remote_config);
        PREFERENCE_EVENTLOGGING_ENABLED = getString(R.string.preference_key_eventlogging_opt_in);
        PREFERENCE_STYLES_LAST_UPDATED = getString(R.string.preference_key_styles_last_updated);
        PREFERENCE_READING_APP_INSTALL_ID = getString(R.string.preference_reading_app_install_id);
        PREFERENCE_ONBOARD = getString(R.string.preference_onboard);
        PREFERENCE_TEXT_SIZE_MULTIPLIER = getString(R.string.preference_text_size_multiplier);
        PREFERENCE_COLOR_THEME = getString(R.string.preference_color_theme);

        THEME_LIGHT = R.style.Theme_WikiLight;
        THEME_DARK = R.style.Theme_WikiDark;

        PROTOCOL = "https"; // Move this to a preference or something later on

        // Enable debugging on the webview
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        Api.setConnectionFactory(new OkHttpConnectionFactory(this));

        if (WikipediaApp.isWikipediaZeroDevmodeOn()) {
            IntentFilter connFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            connChangeReceiver = new ConnectionChangeReceiver();
            this.registerReceiver(connChangeReceiver, connFilter);
        }

        try {
            APP_VERSION_STRING = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // This will never happen!
            throw new RuntimeException(e);
        }

        new PerformMigrationsTask().execute();
    }

    public Bus getBus() {
        return bus;
    }


    private String userAgent;
    public String getUserAgent() {
        if (userAgent == null) {
            userAgent = String.format("WikipediaApp/%s (Android %s; %s)",
                    WikipediaApp.APP_VERSION_STRING,
                    Build.VERSION.RELEASE,
                    getString(R.string.device_type
                    ));
        }
        return userAgent;
    }

    private HashMap<String, Api> apis = new HashMap<String, Api>();
    private MccMncStateHandler mccMncStateHandler = new MccMncStateHandler();
    public Api getAPIForSite(Site site) {
        // http://lists.wikimedia.org/pipermail/wikimedia-l/2014-April/071131.html
        Api api = mccMncStateHandler.makeApiWithMccMncHeaderEnrichment(this, site, getUserAgent());
        if (api != null) {
            return api;
        }

        String domainAndApiDomainKey = site.getDomain() + "-" + site.getApiDomain();

        if (!apis.containsKey(domainAndApiDomainKey))  {
            apis.put(domainAndApiDomainKey, new Api(site.getApiDomain(), getUserAgent()));
        }
        return apis.get(domainAndApiDomainKey);
    }

    private Site primarySite;

    /**
     * Default site of the application
     * You should use PageTitle.getSite() to get the currently browsed site
     */
    public Site getPrimarySite() {
        if (primarySite == null) {
            primarySite = new Site(getPrimaryLanguage() + ".wikipedia.org");
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

    private String primaryLanguage;
    public String getPrimaryLanguage() {
        if (primaryLanguage == null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            primaryLanguage = prefs.getString(PREFERENCE_CONTENT_LANGUAGE, null);
            if (primaryLanguage == null) {
                // No preference set!
                String wikiCode = Utils.langCodeToWikiLang(Locale.getDefault().getLanguage());
                if (!isWikiLanguage(wikiCode)) {
                    wikiCode = "en"; // fallback, see comments in #findWikiIndex
                }
                return wikiCode;
            }
        }
        return primaryLanguage;
    }

    public void setPrimaryLanguage(String language) {
        primaryLanguage = language;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putString(PREFERENCE_CONTENT_LANGUAGE, language).commit();
        primarySite = null;
    }


    private DBOpenHelper dbOpenHelper;
    public DBOpenHelper getDbOpenHelper() {
        if (dbOpenHelper == null) {
            dbOpenHelper = new DBOpenHelper(this);
        }
        return dbOpenHelper;
    }

    private HashMap<String, ContentPersister> persisters = new HashMap<String, ContentPersister>();
    public ContentPersister getPersister(Class cls) {
        if (!persisters.containsKey(cls.getCanonicalName())) {
            ContentPersister persister;
            if (cls.equals(HistoryEntry.class)) {
                persister = new HistoryEntryPersister(this);
            } else if (cls.equals(PageImage.class)) {
                persister = new PageImagePersister(this);
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

    private String[] wikiCodes;
    public int findWikiIndex(String wikiCode) {
        if (wikiCodes == null) {
            wikiCodes = getResources().getStringArray(R.array.preference_language_keys);
        }
        for (int i = 0; i < wikiCodes.length; i++) {
            if (wikiCodes[i].equals(wikiCode)) {
                return i;
            }
        }

        // FIXME: Instrument this with EL to find out what is happening on places where there is a lang we can't find
        // In the meantime, just fall back to en. See https://bugzilla.wikimedia.org/show_bug.cgi?id=66140
        return findWikiIndex("en");
    }

    private boolean isWikiLanguage(String lang) {
        if (wikiCodes == null) {
            wikiCodes = getResources().getStringArray(R.array.preference_language_keys);
        }

        for (String wikiCode : wikiCodes) {
            if (wikiCode.equals(lang)) {
                return true;
            }
        }

        return false;
    }

    private RemoteConfig remoteConfig;
    public RemoteConfig getRemoteConfig() {
        if (remoteConfig == null) {
            remoteConfig = new RemoteConfig(PreferenceManager.getDefaultSharedPreferences(this));
        }
        return remoteConfig;
    }

    private String[] canonicalNames;
    public String canonicalNameFor(int index) {
        if (canonicalNames == null) {
            canonicalNames = getResources().getStringArray(R.array.preference_language_canonical_names);
        }
        return canonicalNames[index];
    }

    private String[] localNames;
    public String localNameFor(int index) {
        if (localNames == null) {
            localNames = getResources().getStringArray(R.array.preference_language_local_names);
        }
        return localNames[index];
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
            cookieManager = new SharedPreferenceCookieManager(PreferenceManager.getDefaultSharedPreferences(this));
        }
        return cookieManager;
    }

    private UserInfoStorage userInfoStorage;
    public UserInfoStorage getUserInfoStorage() {
        if (userInfoStorage == null) {
            userInfoStorage = new UserInfoStorage(PreferenceManager.getDefaultSharedPreferences(this));
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

    private StyleLoader styleLoader;
    public StyleLoader getStyleLoader() {
        if (styleLoader == null) {
            styleLoader = new StyleLoader(this);
        }
        return styleLoader;
    }


    private String appInstallReadActionID;
    public String getAppInstallReadActionID() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.contains(PREFERENCE_READING_APP_INSTALL_ID)) {
            appInstallReadActionID = prefs.getString(PREFERENCE_READING_APP_INSTALL_ID, null);
        } else {
            appInstallReadActionID = UUID.randomUUID().toString();
            prefs.edit().putString(PREFERENCE_READING_APP_INSTALL_ID, appInstallReadActionID).commit();
        }
        Log.d("Wikipedia", "ID is" + appInstallReadActionID);
        return appInstallReadActionID;
    }

    private static boolean W0_DISPOSITION = false;
    public static void setWikipediaZeroDisposition(boolean b) {
        W0_DISPOSITION = b;
    }
    public static boolean getWikipediaZeroDisposition() {
        return W0_DISPOSITION;
    }

    // FIXME: Move this logic elsewhere
    private static String XCS = "";
    public static void setXcs(String s) { XCS = s; }
    public static String getXcs() { return XCS; }

    private static String CARRIER_MESSAGE = "";
    public static void setCarrierMessage(String m) { CARRIER_MESSAGE = m; }
    public static String getCarrierMessage() { return CARRIER_MESSAGE; }

    private static final boolean WIKIPEDIA_ZERO_DEV_MODE_ON = true;
    public static boolean isWikipediaZeroDevmodeOn() {
        return WIKIPEDIA_ZERO_DEV_MODE_ON;
    }

    /**
     * Gets the currently-selected theme for the app.
     * @return Theme that is currently selected, which is the actual theme ID that can
     * be passed to setTheme() when creating an activity.
     */
    public int getCurrentTheme() {
        if (currentTheme == 0) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            currentTheme = prefs.getInt(PREFERENCE_COLOR_THEME, THEME_LIGHT);
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putInt(PREFERENCE_COLOR_THEME, currentTheme).commit();

        //update color filter for logo icon (used in ActionBar activities)...
        adjustDrawableToTheme(getResources().getDrawable(R.drawable.search_w));

        bus.post(new ThemeChangeEvent());
    }

    /**
     * Make adjustments to a Drawable object to look better in the current theme.
     * (e.g. apply a white color filter for night mode)
     * @param d Drawable to be adjusted.
     */
    public void adjustDrawableToTheme(Drawable d) {
        if (getCurrentTheme() == THEME_DARK) {
            d.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        } else {
            d.clearColorFilter();
        }
    }

    public int getFontSizeMultiplier() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getInt(WikipediaApp.PREFERENCE_TEXT_SIZE_MULTIPLIER, 0);
    }

    public void setFontSizeMultiplier(int multiplier) {
        if (multiplier < FONT_SIZE_MULTIPLIER_MIN) {
            multiplier = FONT_SIZE_MULTIPLIER_MIN;
        } else if (multiplier > FONT_SIZE_MULTIPLIER_MAX) {
            multiplier = FONT_SIZE_MULTIPLIER_MAX;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putInt(WikipediaApp.PREFERENCE_TEXT_SIZE_MULTIPLIER, multiplier).commit();
        bus.post(new ChangeTextSizeEvent());
    }

    /**
     * Gets the current size of the app's font. This is given as a device-specific size (not "sp"),
     * and can be passed directly to setTextSize() functions.
     * @param window The window on which the font will be displayed.
     * @return Actual current size of the font.
     */
    public float getFontSize(Window window) {
        int multiplier = PreferenceManager.getDefaultSharedPreferences(this).getInt(WikipediaApp.PREFERENCE_TEXT_SIZE_MULTIPLIER, 0);
        return Utils.getFontSizeFromSp(window, getResources().getDimension(R.dimen.textSize)) * (1.0f + multiplier * 0.1f);
    }
}
