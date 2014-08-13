package org.wikipedia.beta.analytics;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.beta.Site;
import org.wikipedia.beta.WikipediaApp;

import java.util.UUID;

public class SavedPagesFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppSavedPages";
    private static final int REV_ID = 8909354;

    private static final String APP_ID_PREF_NAME = "ANALYTICS_APP_ID_FOR_SAVED_PAGES";

    private final String appInstallSavedPagesID;
    private final Site site;

    public SavedPagesFunnel(WikipediaApp app, Site site) {
        super(app, SCHEMA_NAME, REV_ID);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(app);
        if (prefs.contains(APP_ID_PREF_NAME)) {
            appInstallSavedPagesID = prefs.getString(APP_ID_PREF_NAME, null);
        } else {
            appInstallSavedPagesID = UUID.randomUUID().toString();
            prefs.edit().putString(APP_ID_PREF_NAME, appInstallSavedPagesID).commit();
        }

        this.site = site;
    }

    @Override
    protected JSONObject preprocessData(JSONObject eventData) {
        try {
            eventData.put("savedPagesAppInstallToken", appInstallSavedPagesID);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return eventData;
    }

    protected void log(Object... params) {
        super.log(site, params);
    }

    public void logSaveNew() {
        log(
                "action", "savenew"
        );
    }

    public void logUpdate() {
        log(
                "action", "update"
        );
    }

    public void logImport() {
        log(
                "action", "import"
        );
    }

    public void logDelete() {
        log(
                "action", "delete"
        );
    }

    public void logEditAttempt() {
        log(
                "action", "editattempt"
        );
    }

    public void logEditRefresh() {
        log(
                "action", "editrefresh"
        );
    }

    public void logEditAfterRefresh() {
        log(
                "action", "editafterrefresh"
        );
    }
}
