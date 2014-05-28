package org.wikipedia.analytics;

import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;

public class ProtectedEditAttemptFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppProtectedEditAttempt";
    private static final int REV_ID = 8682497;

    private final Site site;

    public ProtectedEditAttemptFunnel(WikipediaApp app, Site site) {
        super(app, SCHEMA_NAME, REV_ID);
        this.site = site;
    }

    protected void log(Object... params) {
        super.log(site, params);
    }

    public void log(String protectionStatus) {
        log(
                "protectionStatus", protectionStatus
        );
    }
}
