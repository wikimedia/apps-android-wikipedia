package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;

public class ProtectedEditAttemptFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppProtectedEditAttempt";
    private static final int REV_ID = 8682497;

    public ProtectedEditAttemptFunnel(WikipediaApp app, WikiSite wiki) {
        super(app, SCHEMA_NAME, REV_ID, wiki);
    }

    @Override protected void preprocessAppInstallID(@NonNull JSONObject eventData) { }
    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }

    public void log(String protectionStatus) {
        log(
                "protectionStatus", protectionStatus
        );
    }
}
