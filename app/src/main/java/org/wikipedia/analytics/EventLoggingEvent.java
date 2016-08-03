package org.wikipedia.analytics;

import android.net.Uri;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.util.log.L;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Base class for all various types of events that are logged to EventLogging.
 *
 * Each Schema has its own class, and has its own constructor that makes it easy
 * to call from everywhere without having to duplicate param info at all places.
 * Updating schemas / revisions is also easier this way.
 */
public class EventLoggingEvent {
    private static final String EVENTLOG_URL_PROD = "https://meta.wikimedia.org/beacon/event";
    private static final String EVENTLOG_URL_DEV = "http://deployment.wikimedia.beta.wmflabs.org/beacon/event";
    private static final String EVENTLOG_URL = WikipediaApp.getInstance().isPreBetaRelease()
            ? EVENTLOG_URL_DEV : EVENTLOG_URL_PROD;

    private final JSONObject data;
    private final String userAgent;

    /**
     * Create an EventLoggingEvent that logs to a given revision of a given schema with
     * the gven data payload.
     *
     * @param schema Schema name (as specified on meta.wikimedia.org)
     * @param revID Revision of the schema to log to
     * @param wiki DBName (enwiki, dewiki, etc) of the wiki in which we are operating
     * @param userAgent User-Agent string to use for this request
     * @param eventData Data for the actual event payload. Considered to be
     *
     */
    public EventLoggingEvent(String schema, int revID, String wiki, String userAgent, JSONObject eventData) {
        data = new JSONObject();
        try {
            data.put("schema", schema);
            data.put("revision", revID);
            data.put("wiki", wiki);
            data.put("event", eventData);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        this.userAgent = userAgent;
    }

    /**
     * Log the current event.
     *
     * Returns immediately after queueing the network request in the background.
     */
    public void log() {
        new LogEventTask(data).execute();
    }

    private class LogEventTask extends SaneAsyncTask<Integer> {
        private final JSONObject data;

        LogEventTask(JSONObject data) {
            this.data = data;
        }

        @Override
        public Integer performTask() throws Throwable {
            String dataURL = Uri.parse(EVENTLOG_URL)
                    .buildUpon().query(data.toString())
                    .build().toString();
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(dataURL).header("User-Agent", userAgent).build();
            Response response = client.newCall(request).execute();
            try {
                return response.code();
            } finally {
                response.close();
            }
        }

        @Override
        public void onCatch(Throwable caught) {
            // Do nothing bad. EL data is ok to lose.
            L.d("Lost EL data: " + data.toString());
        }
    }
}
