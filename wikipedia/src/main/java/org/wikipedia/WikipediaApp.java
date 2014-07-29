package org.wikipedia;

import android.app.Application;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
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
import org.wikipedia.settings.PrefKeys;

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

    public static float SCREEN_DENSITY;

    // Reload in onCreate to override
    public static String PROTOCOL = "https";

    public static boolean FALLBACK = false;
    public static int FAILS = 0;

    public static String APP_VERSION_STRING;

    public static final int THEME_LIGHT = R.style.Theme_WikiLight;
    public static final int THEME_DARK = R.style.Theme_WikiDark;

    public static final int FONT_SIZE_MULTIPLIER_MIN = -5;
    public static final int FONT_SIZE_MULTIPLIER_MAX = 8;
    private static final float FONT_SIZE_FACTOR = 0.1f;

    /**
     * Singleton instance of WikipediaApp
     */
    private static WikipediaApp INSTANCE;

    private Bus bus;
    private int currentTheme = 0;

    private ConnectionChangeReceiver connChangeReceiver;

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

        final Resources resources = getResources();
        ViewAnimations.init(resources);
        SCREEN_DENSITY = resources.getDisplayMetrics().density;

        PrefKeys.initPreferenceKeys(resources);

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
            String channel = Utils.getChannel(this);
            channel = channel.equals("") ? channel : " ".concat(channel);
            userAgent = String.format("WikipediaApp/%s (Android %s; %s)%s",
                    WikipediaApp.APP_VERSION_STRING,
                    Build.VERSION.RELEASE,
                    getString(R.string.device_type),
                    channel
            );
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
            primaryLanguage = prefs.getString(PrefKeys.getContentLanguageKey(), null);
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
        prefs.edit().putString(PrefKeys.getContentLanguageKey(), language).commit();
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
        if (prefs.contains(PrefKeys.getReadingAppInstallId())) {
            appInstallReadActionID = prefs.getString(PrefKeys.getReadingAppInstallId(), null);
        } else {
            appInstallReadActionID = UUID.randomUUID().toString();
            prefs.edit().putString(PrefKeys.getReadingAppInstallId(), appInstallReadActionID).commit();
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putInt(PrefKeys.getColorTheme(), currentTheme).commit();

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

    /**
     * Make adjustments to a link or button Drawable object to look better in the current theme.
     * (e.g. apply a blue color filter for night mode, )
     * @param d Drawable to be adjusted.
     */
    public void adjustLinkDrawableToTheme(Drawable d) {
        if (getCurrentTheme() == THEME_DARK) {
            d.setColorFilter(getResources().getColor(R.color.button_dark), PorterDuff.Mode.SRC_ATOP);
        } else {
            d.setColorFilter(getResources().getColor(R.color.button_light), PorterDuff.Mode.SRC_ATOP);
        }
    }

    public int getFontSizeMultiplier() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getInt(PrefKeys.getTextSizeMultiplier(), 0);
    }

    public void setFontSizeMultiplier(int multiplier) {
        if (multiplier < FONT_SIZE_MULTIPLIER_MIN) {
            multiplier = FONT_SIZE_MULTIPLIER_MIN;
        } else if (multiplier > FONT_SIZE_MULTIPLIER_MAX) {
            multiplier = FONT_SIZE_MULTIPLIER_MAX;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putInt(PrefKeys.getTextSizeMultiplier(), multiplier).commit();
        bus.post(new ChangeTextSizeEvent());
    }

    /**
     * Gets the current size of the app's font. This is given as a device-specific size (not "sp"),
     * and can be passed directly to setTextSize() functions.
     * @param window The window on which the font will be displayed.
     * @return Actual current size of the font.
     */
    public float getFontSize(Window window) {
        int multiplier = PreferenceManager.getDefaultSharedPreferences(this).getInt(PrefKeys.getTextSizeMultiplier(), 0);
        return Utils.getFontSizeFromSp(window, getResources().getDimension(R.dimen.textSize)) * (1.0f + multiplier * FONT_SIZE_FACTOR);
    }
}
