package org.wikipedia.beta.analytics;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.beta.Site;
import org.wikipedia.beta.WikipediaApp;

import java.util.UUID;

public class ToCInteractionFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppToCInteraction";
    private static final int REV_ID = 8461467;

    private static final String APP_ID_PREF_NAME = "ANALYTICS_APP_ID_FOR_ToC";

    private final String appInstallToCInteractionID;
    private final Site site;

    public ToCInteractionFunnel(WikipediaApp app, Site site) {
        super(app, SCHEMA_NAME, REV_ID);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(app);
        if (prefs.contains(APP_ID_PREF_NAME)) {
            appInstallToCInteractionID = prefs.getString(APP_ID_PREF_NAME, null);
        } else {
            appInstallToCInteractionID = UUID.randomUUID().toString();
            prefs.edit().putString(APP_ID_PREF_NAME, appInstallToCInteractionID).commit();
        }

        this.site = site;
    }

    @Override
    protected JSONObject preprocessData(JSONObject eventData) {
        try {
            eventData.put("tocInteractionToken", appInstallToCInteractionID);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return eventData;
    }

    protected void log(Object... params) {
        super.log(site, params);
    }

    public void logOpen() {
        log(
                "action", "open"
        );
    }

    public void logClose() {
        log(
                "action", "close"
        );
    }

    public void logClick() {
        log(
                "action", "click"
        );
    }
}
