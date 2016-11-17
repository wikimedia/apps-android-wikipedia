package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;

public class InstallReferrerFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppInstallReferrer";
    private static final int REV_ID = 12601905;

    // For an explanation of these parameters, refer to the schema documentation:
    // https://meta.wikimedia.org/wiki/Schema:MobileWikiAppInstallReferrer
    public static final String PARAM_REFERRER_URL = "referrer_url";
    public static final String PARAM_CAMPAIGN_ID = "campaign_id";
    public static final String PARAM_CAMPAIGN_INSTALL_ID = "install_id";
    public static final String PARAM_CHANNEL = "channel";

    public InstallReferrerFunnel(WikipediaApp app) {
        super(app, SCHEMA_NAME, REV_ID);
    }

    public void logInstall(String referrerUrl, String campaignID, String campaignInstallID) {
        log(
                PARAM_REFERRER_URL, referrerUrl,
                PARAM_CAMPAIGN_ID, campaignID,
                PARAM_CAMPAIGN_INSTALL_ID, campaignInstallID
        );
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }
}
