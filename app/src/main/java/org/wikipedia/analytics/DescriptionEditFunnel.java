package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageTitle;

public class DescriptionEditFunnel extends EditFunnel {

    public enum Type {
        NEW("new"),
        EXISTING("existing");

        private String logString;

        Type(@NonNull String logString) {
            this.logString = logString;
        }

        @NonNull public String toLogString() {
            return logString;
        }
    }

    @NonNull private final Type type;

    public DescriptionEditFunnel(@NonNull WikipediaApp app, @NonNull PageTitle title, @NonNull Type type) {
        super(app, title);
        this.type = type;
    }

    @Override public void logStart() {
        log(
                "action", "start",
                "wikidataDescriptionEdit", type.toLogString()
        );
    }

    public void logReady() {
        log(
                "action", "ready",
                "wikidataDescriptionEdit", type.toLogString()
        );
    }

    @Override public void logSaveAttempt() {
        log(
                "action", "saveAttempt",
                "wikidataDescriptionEdit", type.toLogString()
        );
    }

    public void logSaved() {
        log(
                "action", "saved",
                "wikidataDescriptionEdit", type.toLogString()
        );
    }

    @Override public void logAbuseFilterWarning(String code) {
        log(
                "action", "abuseFilterWarning",
                "abuseFilterName", code,
                "wikidataDescriptionEdit", type.toLogString()
        );
    }

    @Override public void logError(String code) {
        log(
                "action", "error",
                "errorText", code,
                "wikidataDescriptionEdit", type.toLogString()
        );
    }
}
