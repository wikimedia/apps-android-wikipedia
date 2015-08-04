package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;

public class SavedPagesFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppSavedPages";
    private static final int REV_ID = 10375480;

    public SavedPagesFunnel(WikipediaApp app, Site site) {
        super(app, SCHEMA_NAME, REV_ID, site);
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

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }
}
