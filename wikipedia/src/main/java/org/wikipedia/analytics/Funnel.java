package org.wikipedia.analytics;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.Site;
import org.wikipedia.Utils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.settings.PrefKeys;

public abstract class Funnel {
    private final String schemaName;
    private final int revision;
    private final WikipediaApp app;

    /**
     * The tag used for any analytics-related events sent to the Log.
     */
    public static final String ANALYTICS_TAG = "Analytics";

    private final SharedPreferences prefs;
    protected Funnel(WikipediaApp app, String schemaName, int revision) {
        this.app = app;
        this.schemaName = schemaName;
        this.revision = revision;

        this.prefs = PreferenceManager.getDefaultSharedPreferences(app);
    }

    protected WikipediaApp getApp() {
        return app;
    }
    /**
     * Optionally pre-process the event data before sending to EL.
     *
     * @param eventData Event Data so far collected
     * @return Event Data to be sent to server
     */
    protected JSONObject preprocessData(JSONObject eventData) {
        return eventData;
    }

    /**
     * Logs an event.
     *
     * @param site    The wiki in which this action was performed.
     * @param params  Actual data for the event. Considered to be an array
     *                of alternating key and value items (for easier
     *                use in subclass constructors).
     *
     *                For example, what would be expressed in a more sane
     *                language as:
     *
     *                .log({
     *                  "page": "List of mass murderers",
     *                  "section": "2014"
     *                });
     *
     *                would be expressed here as
     *
     *                .log(
     *                  "page", "List of mass murderers",
     *                  "section", "2014"
     *                );
     *
     *                This format should be only used in subclass methods directly.
     *                The subclass methods should take more explicit parameters
     *                depending on what they are logging.
     */
    protected void log(Site site, Object... params) {
        if (!prefs.getBoolean(PrefKeys.getEventLoggingEnabled(), true)) {
            // If EL is turned off, this is a NOP
            return;
        }
        JSONObject eventData = new JSONObject();

        //Build the string which is logged to debug EventLogging code
        String logString = this.getClass().getSimpleName() + ": Sending event";
        try {
            for (int i = 0; i < params.length; i += 2) {
                eventData.put(params[i].toString(), params[i + 1]);
                logString += ", event_" + params[i] + " = " + params[i + 1];
            }
            Log.d(ANALYTICS_TAG, logString);
        } catch (JSONException e) {
            // This does not happen
            throw new RuntimeException(e);
        }

        new EventLoggingEvent(
                schemaName,
                revision,
                Utils.getDBNameForSite(site),
                app.getUserAgent(),
                preprocessData(eventData)
        ).log();
    }
}
