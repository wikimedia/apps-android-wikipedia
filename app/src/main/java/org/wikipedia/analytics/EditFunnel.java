package org.wikipedia.analytics;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.page.PageTitle;

public class EditFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppEdit";
    private static final int REV_ID = 16256587;

    private final PageTitle title;

    public EditFunnel(@NonNull WikipediaApp app, @NonNull PageTitle title) {
        super(app, SCHEMA_NAME, REV_ID, title.getWikiSite());
        this.title = title;
    }

    @Nullable
    @Override
    public String getSessionToken() {
        return super.getSessionToken();
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

    @Override
    protected JSONObject preprocessData(@NonNull JSONObject eventData) {
        if (AccountUtil.isLoggedIn()) {
            // noinspection ConstantConditions
            preprocessData(eventData, "userID",
                    AccountUtil.getUserIdForLanguage(title.getWikiSite().languageCode()));
        }
        preprocessData(eventData, "pageNS", title.getNamespace());
        return super.preprocessData(eventData);
    }

    @Override protected void preprocessAppInstallID(@NonNull JSONObject eventData) { }

    @NonNull
    @Override
    protected String getSessionTokenField() {
        return "editSessionToken";
    }
}
