package org.wikipedia.beta.analytics;

import org.wikipedia.beta.WikipediaApp;

public class ConnectionIssueFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppStuffHappens";
    private static final int REVISION = 8955468;

    public ConnectionIssueFunnel(WikipediaApp app) {
        super(app, SCHEMA_NAME, REVISION);
    }

    protected void log(Object... params) {
        super.log(getApp().getPrimarySite(), params);
    }

    public void logConnectionIssue(String failedEndpoint, String applicationContext) {
        log(
                "failedEndpoint", failedEndpoint,
                "applicationContext", applicationContext
        );
    }

}
