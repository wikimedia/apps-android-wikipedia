package org.wikipedia.analytics;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;

public class ToCInteractionFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppToCInteraction";
    private static final int REV_ID = 11014396;

    private final String appInstallID;
    private final Site site;

    public ToCInteractionFunnel(WikipediaApp app, Site site) {
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
        final int defaultSampleRate = 100;

        //get our sampling rate from remote config
        int sampleRate = WikipediaApp.getInstance().getRemoteConfig().getConfig()
                .optInt("tocLogSampleRate", defaultSampleRate);

        if (sampleRate != 0) {
            //take the last 4 hex digits of the uuid, modulo the sampling coefficient.
            //if the result is 0, then we're one of the Chosen.
            final int uuidSubstrLen = 4;
            final int hexBase = 16;
            boolean chosen = Integer.parseInt(appInstallID.substring(appInstallID.length() - uuidSubstrLen), hexBase) % sampleRate == 0;

            if (chosen) {
                super.log(site, params);
            }
        }
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
