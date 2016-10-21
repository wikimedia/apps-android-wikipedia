package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;

import java.util.concurrent.TimeUnit;

/*package*/ abstract class TimedFunnel extends Funnel {
    private long startTime;

    /*package*/ TimedFunnel(WikipediaApp app, String schemaName, int revision, int sampleRate) {
        this(app, schemaName, revision, sampleRate, null);
    }

    /*package*/ TimedFunnel(WikipediaApp app, String schemaName, int revision, int sampleRate, WikiSite wiki) {
        super(app, schemaName, revision, sampleRate, wiki);
        startTime = System.currentTimeMillis();
    }

    @Override
    protected JSONObject preprocessData(@NonNull JSONObject eventData) {
        preprocessData(eventData, getDurationFieldName(), getDurationSeconds());
        return super.preprocessData(eventData);
    }

    /** Override me for deviant implementations. */
    protected String getDurationFieldName() {
        return "timeSpent";
    }

    protected void resetDuration() {
        startTime = System.currentTimeMillis();
    }

    private long getDuration() {
        return System.currentTimeMillis() - startTime;
    }

    private long getDurationSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(getDuration());
    }
}
