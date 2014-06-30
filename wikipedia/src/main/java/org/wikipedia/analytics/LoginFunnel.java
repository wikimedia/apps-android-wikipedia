package org.wikipedia.analytics;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.WikipediaApp;

import java.util.UUID;

public class LoginFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppLogin";
    private static final int REVISION = 8234533;

    public static final String SOURCE_NAV = "navigation";
    public static final String SOURCE_EDIT = "edit";
    public static final String SOURCE_BLOCKED = "blocked";
    public static final String SOURCE_CREATE_ACCOUNT = "create_account";

    private final String loginSessionToken;

    public LoginFunnel(WikipediaApp app) {
        super(app, SCHEMA_NAME, REVISION);
        loginSessionToken = UUID.randomUUID().toString();
    }

    public String getLoginSessionToken() {
        return loginSessionToken;
    }

    protected void log(Object... params) {
        // Login always hits the primarySite anyway.
        log(getApp().getPrimarySite(), params);
    }

    @Override
    protected JSONObject preprocessData(JSONObject eventData) {
        try {
            eventData.put("loginSessionToken", loginSessionToken);
        } catch (JSONException e) {
            // This isn't happening
            throw new RuntimeException(e);
        }
        return eventData;
    }

    public void logStart(String source) {
        logStart(source, null);
    }

    public void logStart(String source, String editSessionToken) {
        log(
                "action", "start",
                "source", source,
                "editSessionToken", editSessionToken
        );
    }

    public void logCreateAccountAttempt() {
        log(
                "action", "createAccountAttempt"
        );
    }

    public void logCreateAccountFailure() {
        log(
                "action", "createAccountFailure"
        );
    }

    public void logCreateAccountSuccess() {
        log(
                "action", "createAccountSuccess"
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
