package org.wikipedia;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.net.*;
import android.os.*;
import android.preference.*;
import android.webkit.*;
import com.squareup.otto.*;
import org.acra.*;
import org.acra.annotation.*;
import org.mediawiki.api.json.*;
import org.wikipedia.data.*;
import org.wikipedia.editing.*;
import org.wikipedia.editing.summaries.*;
import org.wikipedia.history.*;
import org.wikipedia.login.*;
import org.wikipedia.networking.*;
import org.wikipedia.pageimages.*;
import org.wikipedia.savedpages.*;

import java.util.*;


@ReportsCrashes(
        formKey="",
        mode= ReportingInteractionMode.DIALOG,
        resDialogTitle=R.string.acra_report_dialog_title,
        resDialogText=R.string.acra_report_dialog_text,
        resDialogCommentPrompt=R.string.acra_report_dialog_comment,
        mailTo="mobile-android-wikipedia@wikimedia.org")
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
    private static WikipediaApp app;

    public static float SCREEN_DENSITY;
    // Reload in onCreate to override
    public static String PROTOCOL = "https";

    public static String APP_VERSION_STRING;

    private ConnectionChangeReceiver connChangeReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
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

        PROTOCOL = "https"; // Move this to a preference or something later on

        // Enable debugging on the webview
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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

    }

    public Bus getBus() {
        return bus;
    }


    private String userAgent;
    public String getUserAgent() {
        if (userAgent == null) {
            userAgent = String.format("WikipediaApp/%s (Android/%s)", WikipediaApp.APP_VERSION_STRING, Build.VERSION.RELEASE);
        }
        return userAgent;
    }

    private HashMap<String, Api> apis = new HashMap<String, Api>();
    public Api getAPIForSite(Site site) {
        if (!apis.containsKey(site.getDomain()))  {
            apis.put(site.getDomain(), new Api(site.getApiDomain(), getUserAgent()));
        }
        return apis.get(site.getDomain());
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

    private String primaryLanguage;
    public String getPrimaryLanguage() {
        if (primaryLanguage == null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            primaryLanguage = prefs.getString(PREFERENCE_CONTENT_LANGUAGE, null);
            if (primaryLanguage == null) {
                // No preference set!
                String wikiCode = Utils.langCodeToWikiLang(Locale.getDefault().getLanguage());
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
                persister = new SavedPagePerister(this);
            } else if (cls.equals(EditSummary.class)) {
                persister = new EditSummaryPersister(this);
            } else {
                throw new RuntimeException("No persister found for class " + cls.getCanonicalName());
            }
            persisters.put(cls.getCanonicalName(), persister);
        }
        return persisters.get(cls.getCanonicalName());
    }

    private Typeface primaryType;
    public Typeface getPrimaryType() {
        if (primaryType == null) {
            primaryType = Typeface.createFromAsset(getAssets(), "fonts/OpenSans.ttf");
        }
        return primaryType;
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

        throw new RuntimeException("WikiCode " + wikiCode + " + not found+");
    }

    private boolean isWikiLanguage(String lang) {
        if (wikiCodes == null) {
            wikiCodes = getResources().getStringArray(R.array.preference_language_keys);
        }

        for (int i = 0; i < wikiCodes.length; i++) {
            if (wikiCodes[i].equals(lang)) {
                return true;
            }
        }

        return false;
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

    private static boolean wikipediaZeroDisposition = false;
    public static void setWikipediaZeroDisposition(boolean b) {
        wikipediaZeroDisposition = b;
    }
    public static boolean getWikipediaZeroDisposition() {
        return wikipediaZeroDisposition;
    }

    private static String xcs = "";
    public static void setXcs(String s) { xcs = s; }
    public static String getXcs() { return xcs; }

    private static String carrierMessage = "";
    public static void setCarrierMessage(String m) { carrierMessage = m; }
    public static String getCarrierMessage() { return carrierMessage; }

    private static final boolean WIKIPEDIA_ZERO_DEV_MODE_ON = true;
    public static boolean isWikipediaZeroDevmodeOn() {
        return WIKIPEDIA_ZERO_DEV_MODE_ON;
    }
}
