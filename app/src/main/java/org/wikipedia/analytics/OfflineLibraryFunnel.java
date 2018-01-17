package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.offline.Compilation;

import java.util.List;

public class OfflineLibraryFunnel extends TimedFunnel {
    private static final String SCHEMA_NAME = "MobileWikiAppOfflineLibrary";
    private static final int REV_ID = 17649221;

    private final int source;
    private int shareCount;

    public OfflineLibraryFunnel(WikipediaApp app, int source) {
        super(app, SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_ALL, app.getWikiSite());
        this.source = source;
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }

    public void share() {
        shareCount++;
    }

    public void done(@NonNull List<Compilation> currentCompilations) {
        StringBuilder packStr = new StringBuilder();
        for (Compilation c : currentCompilations) {
            if (packStr.length() > 0) {
                packStr.append(",");
            }
            packStr.append(c.name());
        }
        log(
                "source", source,
                "packList", packStr.toString(),
                "shareCount", shareCount
        );
    }
}
