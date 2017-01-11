package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.util.ReleaseUtil;

// https://meta.wikimedia.org/wiki/Schema:MobileWikiAppWiktionaryPopup
public class WiktionaryDialogFunnel extends TimedFunnel {
    private static final String SCHEMA_NAME = "MobileWikiAppWiktionaryPopup";
    private static final int REV_ID = 15158116;

    private final String text;

    public WiktionaryDialogFunnel(WikipediaApp app, String text) {
        super(app, SCHEMA_NAME, REV_ID, ReleaseUtil.isProdRelease() ? Funnel.SAMPLE_LOG_100 : Funnel.SAMPLE_LOG_ALL);
        this.text = text;
    }

    public void logClose() {
        log(
                "text", text
        );
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }
}
