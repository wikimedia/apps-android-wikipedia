package org.wikipedia.analytics;

import org.json.*;
import org.wikipedia.*;

import java.util.*;

public class EditFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppEdit";
    private static final int REV_ID = 8198182;

    private final String editSessionToken;
    private final PageTitle title;

    public EditFunnel(WikipediaApp app, PageTitle title) {
        super(app, SCHEMA_NAME, REV_ID);
        editSessionToken = UUID.randomUUID().toString();
        this.title = title;
    }

    @Override
    protected JSONObject preprocessData(JSONObject eventData) {
        try {
            eventData.put("editSessionToken", editSessionToken);
            if (getApp().getUserInfoStorage().isLoggedIn()) {
                eventData.put("userName", getApp().getUserInfoStorage().getUser().getUsername());
            }
            eventData.put("pageNS", title.getNamespace());
        } catch (JSONException e) {
            // This never happens either
            throw new RuntimeException(e);
        }
        return eventData;
    }

    public String getEditSessionToken() {
        return editSessionToken;
    }

    protected void log(Object... params) {
        super.log(title.getSite(), params);
    }

    public void logStart() {
        log(
                "action", "start"
        );
    }

    public void logPreview() {
        log(
                "action", "preview"
        );
    }

    public void logSaved(int revID) {
        log(
                "action", "saved",
                "revID", revID
        );

    }

    public void logLoginAttempt() {
        log(
                "action", "loginAttempt"
        );

    }

    public void logLoginSuccess() {
        log(
                "action", "loginSuccess"
        );
    }

    public void logLoginFailure() {
        log(
                "action", "loginFailure"
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

    public void logAbuseFilterWarning(String code) {
        log(
                "action", "abuseFilterWarning",
                "abuseFilterCode", code
        );
    }

    public void logAbuseFilterError(String code) {
        log(
                "action", "abuseFilterError",
                "abuseFilterCode", code
        );

    }

    public void logSaveAnonExplicit() {
        log(
                "action", "saveAnonExplicit"
        );
    }

    public void logError(String code) {
        log(
                "action", "error",
                "errorText", code
        );
    }
}
