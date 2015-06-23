package org.wikipedia.analytics;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;

public class InstallReferrerFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppInstallReferrer";
    private static final int REV_ID = 12601905;

    // For an explanation of these parameters, refer to the schema documentation:
    // https://meta.wikimedia.org/wiki/Schema:MobileWikiAppInstallReferrer
    public static final String PARAM_REFERRER_URL = "referrer_url";
    public static final String PARAM_CAMPAIGN_ID = "campaign_id";
    public static final String PARAM_CAMPAIGN_INSTALL_ID = "install_id";

    private final String appInstallID;
    private final Site site;

    public InstallReferrerFunnel(WikipediaApp app, Site site) {
        super(app, SCHEMA_NAME, REV_ID);

        //Retrieve this app installation's unique ID, used to record unique users of features
        appInstallID = app.getAppInstallID();

        this.site = site;
    }

    @Override
    protected JSONObject preprocessData(JSONObject eventData) {
        try {
            eventData.put("appInstallID", appInstallID);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return eventData;
    }

    protected void log(Object... params) {
        super.log(site, params);
    }

    public void logInstall(String referrerUrl, String campaignID, String campaignInstallID) {
        log(
                PARAM_REFERRER_URL, referrerUrl,
                PARAM_CAMPAIGN_ID, campaignID,
                PARAM_CAMPAIGN_INSTALL_ID, campaignInstallID
        );
    }
}
