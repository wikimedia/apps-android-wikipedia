package org.wikipedia.analytics;

import androidx.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;

import java.util.concurrent.TimeUnit;

public abstract class TimedFunnel extends Funnel {
    private long startTime;
    private long pauseTime;

    public TimedFunnel(WikipediaApp app, String schemaName, int revision, int sampleRate) {
        this(app, schemaName, revision, sampleRate, null);
    }

    public TimedFunnel(WikipediaApp app, String schemaName, int revision, int sampleRate, WikiSite wiki) {
        super(app, schemaName, revision, sampleRate, wiki);
        startTime = System.currentTimeMillis();
    }

    @Override
    protected JSONObject preprocessData(@NonNull JSONObject eventData) {
        preprocessData(eventData, getDurationFieldName(), getDurationSeconds());
        return super.preprocessData(eventData);
    }

    public void pause() {
        pauseTime = System.currentTimeMillis();
    }

    public void resume() {
        if (pauseTime > 0) {
            startTime += (System.currentTimeMillis() - pauseTime);
        }
        pauseTime = 0;
    }

    /** Override me for deviant implementations. */
    protected String getDurationFieldName() {
        return "time_spent";
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
