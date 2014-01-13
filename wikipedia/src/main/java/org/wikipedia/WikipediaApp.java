package org.wikipedia;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.webkit.WebView;
import com.github.kevinsawicki.http.HttpRequest;
import com.squareup.okhttp.HttpResponseCache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.otto.Bus;
import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.mediawiki.api.json.Api;
import org.wikipedia.data.ContentPersister;
import org.wikipedia.data.DBOpenHelper;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.history.HistoryEntryPersister;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.pageimages.PageImagePersister;
import org.wikipedia.savedpages.SavedPage;
import org.wikipedia.savedpages.SavedPagePerister;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;

@ReportsCrashes(
        formKey="",
        mode= ReportingInteractionMode.DIALOG,
        resDialogTitle=R.string.acra_report_dialog_title,
        resDialogText=R.string.acra_report_dialog_text,
        resDialogCommentPrompt=R.string.acra_report_dialog_comment,
        mailTo="yuvipanda@wikimedia.org")
public class WikipediaApp extends Application {
    private Bus bus;

    public static long SHORT_ANIMATION_DURATION;
    public static long MEDIUM_ANIMATION_DURATION;

    public static String PREFERENCE_CONTENT_LANGUAGE;

    public static float SCREEN_DENSITY;
    // Reload in onCreate to override
    public static String PROTOCOL = "https";

    @Override
    public void onCreate() {
        super.onCreate();
        ACRA.init(this);

        bus = new Bus();

        SHORT_ANIMATION_DURATION = getResources().getInteger(android.R.integer.config_shortAnimTime);
        MEDIUM_ANIMATION_DURATION = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        SCREEN_DENSITY = getResources().getDisplayMetrics().density;

        PREFERENCE_CONTENT_LANGUAGE = getResources().getString(R.string.preference_key_language);

        PROTOCOL = "https"; // Move this to a preference or something later on

        // Enable debugging on the webview
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        Api.setConnectionFactory(new OkHttpConnectionFactory(this));
    }

    public Bus getBus() {
        return bus;
    }

    private HashMap<String, Api> apis = new HashMap<String, Api>();
    public Api getAPIForSite(Site site) {
        if (!apis.containsKey(site.getDomain()))  {
            apis.put(site.getDomain(), new Api(site.getDomain()));
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
            primaryLanguage = prefs.getString(PREFERENCE_CONTENT_LANGUAGE, Utils.getDefaultContentLanguage());
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
            } else {
                throw new RuntimeException("No persister found for class " + cls.getCanonicalName());
            }
            persisters.put(cls.getCanonicalName(), persister);
        }
        return persisters.get(cls.getCanonicalName());
    }
}
