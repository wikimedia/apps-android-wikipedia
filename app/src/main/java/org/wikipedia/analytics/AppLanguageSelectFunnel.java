package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;

public class AppLanguageSelectFunnel extends TimedFunnel {
    private static final String SCHEMA_NAME = "MobileWikiAppLangSelect";
    private static final int REV_ID = 12588733;

    private final boolean initiatedFromSearchBar;
    private final String previousLanguage;

    public AppLanguageSelectFunnel(boolean initiatedFromSearchBar) {
        this(initiatedFromSearchBar, WikipediaApp.getInstance().getAppOrSystemLanguageCode());
    }

    public AppLanguageSelectFunnel(boolean initiatedFromSearchBar, String previousLanguage) {
        super(WikipediaApp.getInstance(), SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_100);
        this.initiatedFromSearchBar = initiatedFromSearchBar;
        this.previousLanguage = previousLanguage;
    }

    public void logStart() {
        log("start");
    }

    public void logSelect() {
        logSelect(null);
    }

    public void logSelect(String newLanguage) {
        log("select", newLanguage);
    }

    public void logCancel() {
        log("cancel");
    }

    public void log(String action) {
        log(action, null);
    }

    private void log(String action, String newLanguage) {
        log(
                "action", action,
                "newLang", newLanguage == null ? getApp().getAppOrSystemLanguageCode() : newLanguage
        );
    }

    @Override
    protected JSONObject preprocessData(@NonNull JSONObject eventData) {
        preprocessData(eventData, "source", initiatedFromSearchBar ? 1 : 0);
        preprocessData(eventData, "oldLang", previousLanguage);
        return super.preprocessData(eventData);
    }
}
