package org.wikipedia.analytics;

import androidx.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;

import java.util.concurrent.TimeUnit;

public class ToCInteractionFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppToCInteraction";
    private static final int REV_ID = 19044853;

    private final int pageId;
    private final int numSections;

    private int numPeeks;
    private int numOpens;
    private int numSectionClicks;

    private long lastPeekMillis;
    private int totalPeekSec;

    private long lastScrollStartMillis;
    private int totalOpenedSec;

    public ToCInteractionFunnel(WikipediaApp app, WikiSite wiki, int pageId, int numSections) {
        super(app, SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_ALL, wiki);
        this.pageId = pageId;
        this.numSections = numSections;
    }

    @Override
    protected JSONObject preprocessData(@NonNull JSONObject eventData) {
        preprocessData(eventData, "page_id", pageId);
        preprocessData(eventData, "num_sections", numSections);
        return super.preprocessData(eventData);
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }

    public void peek() {
        numPeeks++;
        lastPeekMillis = System.currentTimeMillis();
    }

    public void hide() {
        if (lastPeekMillis == 0) {
            return;
        }
        totalPeekSec += (System.currentTimeMillis() - lastPeekMillis) / TimeUnit.SECONDS.toMillis(1);
        lastPeekMillis = 0;
    }

    public void scrollStart() {
        numOpens++;
        lastScrollStartMillis = System.currentTimeMillis();
    }

    public void scrollStop() {
        if (lastScrollStartMillis == 0) {
            return;
        }
        totalOpenedSec += (System.currentTimeMillis() - lastScrollStartMillis) / TimeUnit.SECONDS.toMillis(1);
        lastScrollStartMillis = 0;
    }


    public void logClick() {
        numSectionClicks++;
    }

    public void log() {
        hide();
        scrollStop();
        if (numSections == 0 || totalPeekSec == 0) {
            return;
        }
        log(
                "num_peeks", numPeeks,
                "num_opens", numOpens,
                "num_section_clicks", numSectionClicks,
                "total_peek_sec", totalPeekSec,
                "total_open_sec", totalOpenedSec
        );
    }
}
