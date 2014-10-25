package org.wikipedia.analytics;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;

public class SavedPagesFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppSavedPages";
    private static final int REV_ID = 8909354;

    private final String appInstallSavedPagesID;
    private final Site site;

    public SavedPagesFunnel(WikipediaApp app, Site site) {
        super(app, SCHEMA_NAME, REV_ID);

        //Retrieve this app installation's unique ID, used to record unique users of this feature
        appInstallSavedPagesID = app.getAppInstallSavedPagesID();

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
