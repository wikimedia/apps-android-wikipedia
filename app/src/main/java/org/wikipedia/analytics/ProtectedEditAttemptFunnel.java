package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;

public class ProtectedEditAttemptFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppProtectedEditAttempt";
    private static final int REV_ID = 8682497;

    public ProtectedEditAttemptFunnel(WikipediaApp app, Site site) {
        super(app, SCHEMA_NAME, REV_ID, site);
    }

    @Override protected void preprocessAppInstallID(@NonNull JSONObject eventData) { }
    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }

    public void log(String protectionStatus) {
        log(
                "protectionStatus", protectionStatus
        );
    }
}
