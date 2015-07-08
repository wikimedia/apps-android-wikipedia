package org.wikipedia.analytics;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.Site;
import org.wikipedia.Utils;
import org.wikipedia.WikipediaApp;

import java.util.UUID;

/** Schemas for this abstract funnel are expected to have appInstallID and sessionToken fields. When
 * these fields are not present or differently named, preprocess* or get*Field should be overridden. */
/*package*/ abstract class Funnel {
    private static final int SAMPLE_LOG_ALL = 1;
    private static final String DEFAULT_APP_INSTALL_ID_KEY = "appInstallID";
    private static final String DEFAULT_SESSION_TOKEN_KEY = "sessionToken";

    private final String schemaName;
    private final int revision;
    private final WikipediaApp app;
    @Nullable private final Site site;

    private final String sessionToken = UUID.randomUUID().toString();

    /**
     * The tag used for any analytics-related events sent to the Log.
     */
    public static final String ANALYTICS_TAG = "Analytics";

    /*package*/ Funnel(WikipediaApp app, String schemaName, int revision) {
        this(app, schemaName, revision, null);
    }

    /*package*/ Funnel(WikipediaApp app, String schemaName, int revision, @Nullable Site site) {
        this.app = app;
        this.schemaName = schemaName;
        this.revision = revision;
        this.site = site;
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
    protected JSONObject preprocessData(@NonNull JSONObject eventData) {
        preprocessAppInstallID(eventData);
        preprocessSessionToken(eventData);
        return eventData;
    }

    /** Invokes {@link JSONObject#put} on <code>data</code> and throws a {@link RuntimeException} on
     * failure. */
    protected <T> void preprocessData(@NonNull JSONObject eventData, String key, T val) {
        try {
            eventData.put(key, val);
        } catch (JSONException e) {
            throw new RuntimeException("key=" + key + " val=" + val, e);
        }
    }

    /** Invoked by {@link #preprocessData(JSONObject)}. */
    protected void preprocessAppInstallID(@NonNull JSONObject eventData) {
        preprocessData(eventData, getAppInstallIDField(), getAppInstallID());
    }

    /** Invoked by {@link #preprocessData(JSONObject)}. */
    protected void preprocessSessionToken(@NonNull JSONObject eventData) {
        preprocessData(eventData, getSessionTokenField(), getSessionToken());
    }

    protected void log(Object... params) {
        log(SAMPLE_LOG_ALL, params);
    }

    protected void log(Site site, Object... params) {
        log(site, SAMPLE_LOG_ALL, params);
    }

    protected void log(int rate, Object... params) {
        log(site, rate, params);
    }

    /**
     * Logs an event.
     *
     * @param rate          The sampling rate.
     * @param params        Actual data for the event. Considered to be an array
     *                      of alternating key and value items (for easier
     *                      use in subclass constructors).
     *
     *                      For example, what would be expressed in a more sane
     *                      language as:
     *
     *                      .log({
     *                          "page": "List of mass murderers",
     *                          "section": "2014"
     *                      });
     *
     *                      would be expressed here as
     *
     *                      .log(
     *                          "page", "List of mass murderers",
     *                          "section", "2014"
     *                      );
     *
     *                      This format should be only used in subclass methods directly.
     *                      The subclass methods should take more explicit parameters
     *                      depending on what they are logging.
     */
    protected void log(@Nullable Site site, int rate, Object... params) {
        if (!app.isEventLoggingEnabled()) {
            // Do not send events if the user opted out of EventLogging
            return;
        }

        if (rate != 0) {
            boolean chosen = app.getEventLogSamplingID() % rate == 0 || app.isDevRelease();

            if (chosen) {
                JSONObject eventData = new JSONObject();

                //Build the string which is logged to debug EventLogging code
                String logString = this.getClass().getSimpleName() + ": Sending event";
                for (int i = 0; i < params.length; i += 2) {
                    preprocessData(eventData, params[i].toString(), params[i + 1]);
                    logString += ", event_" + params[i] + " = " + params[i + 1];
                }
                Log.d(ANALYTICS_TAG, logString);

                new EventLoggingEvent(
                        schemaName,
                        revision,
                        Utils.getDBNameForSite(site == null ? getApp().getPrimarySite() : site),
                        app.getUserAgent(),
                        preprocessData(eventData)
                ).log();
            }
        }
    }

    /** @return The application installation identifier field used by {@link #preprocessAppInstallID}. */
    @NonNull protected String getAppInstallIDField() {
        return DEFAULT_APP_INSTALL_ID_KEY;
    }

    /** @return The session identifier field used by {@link #preprocessSessionToken}. */
    @NonNull protected String getSessionTokenField() {
        return DEFAULT_SESSION_TOKEN_KEY;
    }

    /** @return The application installation identifier used by {@link #preprocessAppInstallID}. */
    @Nullable protected String getAppInstallID() {
        return getApp().getAppInstallID();
    }

    /** @return The session identifier used by {@link #preprocessSessionToken}. */
    @Nullable protected String getSessionToken() {
        return sessionToken;
    }
}
