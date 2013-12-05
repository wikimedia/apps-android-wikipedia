package org.wikimedia.wikipedia;

import android.app.Application;
import com.squareup.otto.Bus;
import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.mediawiki.api.json.Api;
import org.wikimedia.wikipedia.data.ContentPersister;
import org.wikimedia.wikipedia.data.DBOpenHelper;
import org.wikimedia.wikipedia.history.HistoryEntry;
import org.wikimedia.wikipedia.history.HistoryEntryPersister;

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

    public static float SCREEN_DENSITY;

    @Override
    public void onCreate() {
        super.onCreate();
        ACRA.init(this);

        bus = new Bus();

        SHORT_ANIMATION_DURATION = getResources().getInteger(android.R.integer.config_shortAnimTime);
        MEDIUM_ANIMATION_DURATION = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        SCREEN_DENSITY = getResources().getDisplayMetrics().density;
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
            // FIXME: Actually read from SharedPreferences or something
            primarySite = new Site("en.wikipedia.org");
        }
        return primarySite;
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
            } else {
                throw new RuntimeException("No persister found for class " + cls.getCanonicalName());
            }
            persisters.put(cls.getCanonicalName(), persister);
        }
        return persisters.get(cls.getCanonicalName());
    }
}
