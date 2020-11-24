package org.wikipedia.analytics;

import androidx.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;

public class CreateAccountFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppCreateAccount";
    private static final int REVISION = 20709917;

    private final String requestSource;

    public CreateAccountFunnel(WikipediaApp app, String requestSource) {
        super(app, SCHEMA_NAME, REVISION);
        this.requestSource = requestSource;
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

    @Override
    protected JSONObject preprocessData(@NonNull JSONObject eventData) {
        preprocessData(eventData, "source", requestSource);
        return super.preprocessData(eventData);
    }
}
