package org.wikipedia.analytics;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;

/**
 * Schema: https://meta.wikimedia.org/wiki/Schema:MobileWikiAppLogin
 */
public class LoginFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppLogin";
    private static final int REVISION = 9135390;

    public static final String SOURCE_NAV = "navigation";
    public static final String SOURCE_EDIT = "edit";
    public static final String SOURCE_BLOCKED = "blocked";
    public static final String SOURCE_SYSTEM = "system";
    public static final String SOURCE_ONBOARDING = "onboarding";
    public static final String SOURCE_SETTINGS = "settings";

    public LoginFunnel(WikipediaApp app) {
        super(app, SCHEMA_NAME, REVISION);
    }

    @Nullable
    @Override
    public String getSessionToken() {
        return super.getSessionToken();
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

    @Override protected void preprocessAppInstallID(@NonNull JSONObject eventData) { }

    @NonNull
    @Override
    protected String getSessionTokenField() {
        return "loginSessionToken";
    }
}
