package org.wikipedia.analytics;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.WikipediaApp;

import java.util.ArrayList;
import java.util.List;

public class TabFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppTabs";
    private static final int SCHEMA_REVISION = 12453651;

    public TabFunnel() {
        super(WikipediaApp.getInstance(), SCHEMA_NAME, SCHEMA_REVISION, Funnel.SAMPLE_LOG_100);
    }

    public void logOpenInNew(int size) {
        log("openInNew", size);
    }

    public void logEnterList(int size) {
        log("enterList", size);
    }

    public void logCreateNew(int size) {
        log("createNew", size);
    }

    public void logClose(int size, int index) {
        log("close", size, index);
    }

    public void logSelect(int size, int index) {
        log("select", size, index);
    }

    public void logCancel(int size) {
        log("cancel", size);
    }

    @NonNull
    @Override
    protected String getSessionTokenField() {
        return "tabsSessionToken";
    }

    private void log(String action, int size) {
        log(action, size, null);
    }

    private void log(String action, int size, @Nullable Integer index) {
        List<Object> params = new ArrayList<>();
        params.add("action"); params.add(action);
        params.add("tabCount"); params.add(size);
        if (index != null) {
            params.add("tabIndex"); params.add(index);
        }
        log(params.toArray());
    }
}
