package org.wikipedia.beta.analytics;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.beta.WikipediaApp;

import java.util.UUID;

/**
 * Schema: https://meta.wikimedia.org/wiki/Schema:MobileWikiAppOnboarding
 */
public class OnboardingFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppOnboarding";
    private static final int REVISION = 9123466;

    private static final String ACTION = "action";
    private static final String ACTION_START = "start";
    private static final String ACTION_LOGIN = "login";
    private static final String ACTION_CREATE_ACCOUNT = "create-account";
    private static final String ACTION_SKIP = "skip";

    private final String onboardingToken;

    public OnboardingFunnel(WikipediaApp app) {
        super(app, SCHEMA_NAME, REVISION);
        onboardingToken = UUID.randomUUID().toString();
    }

    protected void log(Object... params) {
        // Login always hits the primarySite anyway.
        log(getApp().getPrimarySite(), params);
    }

    @Override
    protected JSONObject preprocessData(JSONObject eventData) {
        try {
            eventData.put("onboardingToken", onboardingToken);
        } catch (JSONException e) {
            // This isn't happening
            throw new RuntimeException(e);
        }
        return eventData;
    }

    public void logStart() {
        log(
                ACTION, ACTION_START
        );
    }

    public void logCreateAccount() {
        log(
                ACTION, ACTION_CREATE_ACCOUNT
        );
    }

    public void logLogin() {
        log(
                ACTION, ACTION_LOGIN
        );
    }

    public void logSkip() {
        log(
                ACTION, ACTION_SKIP
        );
    }
}
