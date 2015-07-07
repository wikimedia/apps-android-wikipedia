package org.wikipedia.analytics;

import org.wikipedia.WikipediaApp;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;

public abstract class TimedFunnel extends Funnel {
    private final long startTime;

    public TimedFunnel(WikipediaApp app, String schemaName, int revision) {
        super(app, schemaName, revision);
        startTime = System.currentTimeMillis();
    }

    @Override
    protected JSONObject preprocessData(JSONObject eventData) {
        try {
            return super.preprocessData(eventData).put(getDurationFieldName(), getDurationSeconds());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    /** Override me for deviant implementations. */
    protected String getDurationFieldName() {
        return "timeSpent";
    }

    private long getDuration() {
        return System.currentTimeMillis() - startTime;
    }

    private long getDurationSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(getDuration());
    }
}
