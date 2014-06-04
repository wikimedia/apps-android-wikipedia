package org.wikipedia.analytics;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.WikipediaApp;

import java.util.UUID;

public class CreateAccountFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppCreateAccount";
    private static final int REVISION = 8240702;

    private final String createAccountSessionToken;

    public CreateAccountFunnel(WikipediaApp app) {
        super(app, SCHEMA_NAME, REVISION);
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
