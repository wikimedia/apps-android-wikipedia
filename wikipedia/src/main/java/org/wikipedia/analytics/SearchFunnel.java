package org.wikipedia.analytics;

import org.wikipedia.WikipediaApp;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.UUID;

public class SearchFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppSearch";
    private static final int REVISION = 10641988;

    private final String searchSessionToken;
    private final String appInstallID;

    public SearchFunnel(WikipediaApp app) {
        super(app, SCHEMA_NAME, REVISION);
        //Retrieve this app installation's unique ID, used to record unique users of features
        appInstallID = app.getAppInstallID();
        searchSessionToken = UUID.randomUUID().toString();
    }

    protected void log(Object... params) {
        final int defaultSampleRate = 100;

        //get our sampling rate from remote config
        int sampleRate = WikipediaApp.getInstance().getRemoteConfig().getConfig()
                                     .optInt("searchLogSampleRate", defaultSampleRate);

        if (sampleRate != 0) {
            //take the last 4 hex digits of the uuid, modulo the sampling coefficient.
            //if the result is 0, then we're one of the Chosen.
            final int uuidSubstrLen = 4;
            final int hexBase = 16;
            boolean chosen = Integer.parseInt(appInstallID.substring(appInstallID.length() - uuidSubstrLen), hexBase) % sampleRate == 0;

            if (chosen) {
                super.log(getApp().getPrimarySite(), params);
            }
        }
    }

    @Override
    protected JSONObject preprocessData(JSONObject eventData) {
        try {
            eventData.put("appInstallID", appInstallID);
            eventData.put("searchSessionToken", searchSessionToken);
        } catch (JSONException e) {
            // This isn't happening
            throw new RuntimeException(e);
        }
        return eventData;
    }

    public void searchStart() {
        log(
                "action", "start"
        );
    }

    public void searchCancel() {
        log(
                "action", "cancel"
        );
    }

    public void searchClick() {
        log(
                "action", "click"
        );
    }

    public void searchAutoSwitch() {
        log(
                "action", "autoswitch"
        );
    }

    public void searchDidYouMean() {
        log(
                "action", "didyoumean"
        );
    }

    public void searchResults(boolean fullText, int numResults, int delayMillis) {
        log(
                "action", "results",
                "typeOfSearch", fullText ? "full" : "prefix",
                "numberOfResults", numResults,
                "timeToDisplayResults", delayMillis
        );
    }

    public void searchError(boolean fullText, int delayMillis) {
        log(
                "action", "error",
                "typeOfSearch", fullText ? "full" : "prefix",
                "timeToDisplayResults", delayMillis
        );
    }
}
