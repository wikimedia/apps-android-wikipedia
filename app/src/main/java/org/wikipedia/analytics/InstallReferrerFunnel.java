package org.wikipedia.analytics;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;

public class InstallReferrerFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppInstallReferrer";
    private static final int REV_ID = 18115554;

    // For an explanation of these parameters, refer to the schema documentation:
    // https://meta.wikimedia.org/wiki/Schema:MobileWikiAppInstallReferrer
    static final String PARAM_REFERRER_URL = "referrer_url";
    static final String PARAM_UTM_MEDIUM = "utm_medium";
    static final String PARAM_UTM_CAMPAIGN = "utm_campaign";
    static final String PARAM_UTM_SOURCE = "utm_source";
    static final String PARAM_CHANNEL = "channel";

    InstallReferrerFunnel(WikipediaApp app) {
        super(app, SCHEMA_NAME, REV_ID);
    }

    void logInstall(@Nullable String referrerUrl, @Nullable String utfMedium,
                           @Nullable String utfCampaign, @Nullable String utfSource) {
        log(
                PARAM_REFERRER_URL, referrerUrl,
                PARAM_UTM_MEDIUM, utfMedium,
                PARAM_UTM_CAMPAIGN, utfCampaign,
                PARAM_UTM_SOURCE, utfSource
        );
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }
}
