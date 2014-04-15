package org.wikipedia.eventlogging;

import android.net.*;
import android.util.*;
import com.github.kevinsawicki.http.*;
import org.json.*;
import org.wikipedia.concurrency.*;

/**
 * Base class for all various types of events that are logged to EventLogging.
 *
 * Each Schema has its own class, and has its own constructor that makes it easy
 * to call from everywhere without having to duplicate param info at all places.
 * Updating schemas / revisions is also easier this way.
 */
public abstract class EventLoggingEvent {
    private static final String EVENTLOG_URL = "https://bits.wikimedia.org/event.gif";

    private final JSONObject data;

    /**
     * Create an EventLoggingEvent that logs to a given revision of a given schema with
     * the gven data payload.
     *
     * @param schema Schema name (as specified on meta.wikimedia.org)
     * @param revID Revision of the schema to log to
     * @param payload Data for the actual event payload. Considered to be
     *                an array of alternating key and value items (for easier
     *                construction in subclass constructors).
     *
     *                For example, what would be expressed in a more sane
     *                language as:
     *
     *                new SomeSubClass("Schema", 4200, {
     *                  "page": "List of mass murderers",
     *                  "section": "2014"
     *                });
     *
     *                would be expressed here as
     *
     *                new SomeSubClass("Schema", 4200,
     *                  "page", "List of mass murderers",
     *                  "section", "2014"
     *                );
     *
     *                This format should be only used in subclass constructors.
     *                The subclass constructors should take more explicit parameters
     *                depending on what they are logging.
     */
    protected EventLoggingEvent(String schema, int revID, String... payload) {
        data = new JSONObject();
        try {
            data.put("schema", schema);
            data.put("revision", revID);

            JSONObject event = new JSONObject();

            for (int i = 0; i < payload.length; i += 2) {
                event.put(payload[i], payload[i + 1]);
            }

            data.put("event", event);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Log the current event.
     *
     * Returns immediately after queueing the network request in the background.
     */
    public void log() {
        new LogEventTask(data).execute();
    }

    private static class LogEventTask extends SaneAsyncTask<Boolean> {
        private final JSONObject data;
        public LogEventTask(JSONObject data) {
            super(1);
            this.data = data;
        }

        @Override
        public Boolean performTask() throws Throwable {
            String dataURL = Uri.parse(EVENTLOG_URL)
                    .buildUpon().query(data.toString())
                    .build().toString();
            return HttpRequest.get(dataURL).ok();
        }

        @Override
        public void onCatch(Throwable caught) {
            // Do nothing bad. EL data is ok to lose.
            Log.d("Wikipedia", "Lost EL data: " + data.toString());
        }
    }
}
