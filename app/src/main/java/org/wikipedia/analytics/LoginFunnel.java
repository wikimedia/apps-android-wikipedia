package org.wikipedia.analytics;

import org.wikipedia.WikipediaApp;

/**
 * Schema: https://meta.wikimedia.org/wiki/Schema:MobileWikiAppLogin
 */
public class LoginFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppLogin";
    private static final int REVISION = 18144592;

    public static final String SOURCE_NAV = "navigation";
    public static final String SOURCE_EDIT = "edit";
    public static final String SOURCE_BLOCKED = "blocked";
    public static final String SOURCE_SYSTEM = "system";
    public static final String SOURCE_ONBOARDING = "onboarding";
    public static final String SOURCE_SETTINGS = "settings";
    public static final String SOURCE_READING_MANUAL_SYNC = "reading_lists_manual_sync";

    public LoginFunnel(WikipediaApp app) {
        super(app, SCHEMA_NAME, REVISION);
    }

    public void logStart(String source) {
        logStart(source, null);
    }

    public void logStart(String source, String editSessionToken) {
        log(
                "action", "start",
                "source", source,
                "edit_session_token", editSessionToken
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
                "error_text", code
        );
    }

    public void logSuccess() {
        log(
                "action", "success"
        );
    }
}
