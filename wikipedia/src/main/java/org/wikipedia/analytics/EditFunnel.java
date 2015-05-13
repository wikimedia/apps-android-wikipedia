package org.wikipedia.analytics;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.page.PageTitle;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;

import java.util.UUID;

public class EditFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppEdit";
    private static final int REV_ID = 9003125;

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
                if (getApp().getUserInfoStorage().getUser().getUserID() == 0) {
                    // Means we are logged in, but before we started counting UserID.
                    // Send -1 to record these
                    eventData.put("userID", -1);
                } else {
                    eventData.put("userID", getApp().getUserInfoStorage().getUser().getUserID());
                }
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
                "abuseFilterName", code
        );
    }


    public void logAbuseFilterWarningIgnore(String code) {
        log(
                "action", "abuseFilterWarningIgnore",
                "abuseFilterName", code
        );
    }

    public void logAbuseFilterWarningBack(String code) {
        log(
                "action", "abuseFilterWarningBack",
                "abuseFilterName", code
        );
    }

    public void logAbuseFilterError(String code) {
        log(
                "action", "abuseFilterError",
                "abuseFilterName", code
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

    /**
     * Log a summary being tapped.
     *
     * @param summaryTagStringID String resource id of the summary tag tapped
     */
    public void logEditSummaryTap(int summaryTagStringID) {
        String summaryTag;
        switch (summaryTagStringID) {
            case R.string.edit_summary_tag_typo:
                summaryTag = "typo";
                break;
            case R.string.edit_summary_tag_grammar:
                summaryTag = "grammar";
                break;
            case R.string.edit_summary_tag_links:
                summaryTag = "links";
                break;
            case R.string.edit_summary_tag_other:
                summaryTag = "other";
                break;
            default:
                // Unknown summary tag. Must throw exception so whoever is testing
                // can add the entry here
                throw new RuntimeException("Need to add new summary tags to EditFunnel");
        }

        log(
                "action", "editSummaryTap",
                "editSummaryTapped", summaryTag
        );
    }

    public void logSaveAttempt() {
        log(
                "action", "saveAttempt"
        );
    }

}
