package org.wikipedia.beta.analytics;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.beta.WikipediaApp;

import java.util.UUID;

public class CreateAccountFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppCreateAccount";
    private static final int REVISION = 9135391;

    private final String createAccountSessionToken;
    private final String requestSource;

    public CreateAccountFunnel(WikipediaApp app, String requestSource) {
        super(app, SCHEMA_NAME, REVISION);
        this.requestSource = requestSource;
        createAccountSessionToken = UUID.randomUUID().toString();
    }

    protected void log(Object... params) {
        // Create Account always hits the primarySite anyway.
        log(getApp().getPrimarySite(), params);
    }

    @Override
    protected JSONObject preprocessData(JSONObject eventData) {
        try {
            eventData.put("createAccountSessionToken", createAccountSessionToken);
            eventData.put("source", requestSource);
        } catch (JSONException e) {
            // This isn't happening
            throw new RuntimeException(e);
        }
        return eventData;
    }

    public void logStart(String loginSessionToken) {
        log(
                "action", "start",
                "loginSessionToken", loginSessionToken
        );
    }

    public void logCaptchaShown() {
        log(
                "action", "captchaShown"
        );
    }

    public void logCaptchaFailure() {
        log(
                "action", "captchaFailure"
        );
    }

    public void logError(String code) {
        log(
                "action", "error",
                "errorText", code
        );
    }

    public void logSuccess() {
        log(
                "action", "success"
        );
    }
}
