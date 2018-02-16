package org.wikipedia.analytics;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.util.ReleaseUtil;
import org.wikipedia.util.log.L;

import java.util.UUID;

/** Schemas for this abstract funnel are expected to have appInstallID and sessionToken fields. When
 * these fields are not present or differently named, preprocess* or get*Field should be overridden. */
abstract class Funnel {
    protected static final int SAMPLE_LOG_1K = 1000;
    protected static final int SAMPLE_LOG_100 = 100;
    protected static final int SAMPLE_LOG_10 = 10;
    protected static final int SAMPLE_LOG_ALL = 1;

    private static final String DEFAULT_APP_INSTALL_ID_KEY = "appInstallID";
    private static final String DEFAULT_SESSION_TOKEN_KEY = "sessionToken";

    private final String schemaName;
    private final int revision;
    private final int sampleRate;
    private final String sampleRateRemoteParamName;
    private final WikipediaApp app;
    // todo: remove @SerializedName if not pickled
    @SerializedName("site") @Nullable private final WikiSite wiki;

    private final String sessionToken = UUID.randomUUID().toString();

    /*package*/ Funnel(WikipediaApp app, String schemaName, int revision) {
        this(app, schemaName, revision, SAMPLE_LOG_ALL);
    }

    /*package*/ Funnel(WikipediaApp app, String schemaName, int revision, @Nullable WikiSite wiki) {
        this(app, schemaName, revision, SAMPLE_LOG_ALL, wiki);
    }

    /*package*/ Funnel(WikipediaApp app, String schemaName, int revision, int sampleRate) {
        this(app, schemaName, revision, sampleRate, null);
    }

    /*package*/ Funnel(WikipediaApp app, String schemaName, int revision, int sampleRate, @Nullable WikiSite wiki) {
        this.app = app;
        this.schemaName = schemaName;
        this.revision = revision;
        this.sampleRate = sampleRate;
        this.wiki = wiki;
        sampleRateRemoteParamName = schemaName + "_rate";
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
        preprocessData(eventData, getAppInstallIDField(), app.getAppInstallID());
    }

    /** Invoked by {@link #preprocessData(JSONObject)}. */
    protected void preprocessSessionToken(@NonNull JSONObject eventData) {
        preprocessData(eventData, getSessionTokenField(), getSessionToken());
    }

    protected void log(Object... params) {
        log(wiki, params);
    }

    /**
     * Logs an event.
     *
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
    protected void log(@Nullable WikiSite wiki, Object... params) {
        if (ReleaseUtil.isDevRelease()
                || isUserInSamplingGroup(app.getAppInstallID(), getSampleRate())) {
            JSONObject eventData = new JSONObject();

            //Build the string which is logged to debug EventLogging code
            String logString = this.getClass().getSimpleName() + ": Sending event";
            for (int i = 0; i < params.length; i += 2) {
                preprocessData(eventData, params[i].toString(), params[i + 1]);
                logString += ", event_" + params[i] + " = " + params[i + 1];
            }
            L.d(logString);

            EventLoggingEvent event = new EventLoggingEvent(
                    schemaName,
                    revision,
                    wiki == null ? app.getWikiSite().dbName() : wiki.dbName(),
                    preprocessData(eventData)
            );
            EventLoggingService.getInstance().log(event.getData());
        }
    }

    /**
     * @return Sampling rate for this funnel, as given by the remote config parameter for this
     * funnel (the name of which is defined as "[schema name]_rate"), with a fallback to the
     * hard-coded sampling rate passed into the constructor.
     */
    private int getSampleRate() {
        return app.getRemoteConfig().getConfig().optInt(sampleRateRemoteParamName, sampleRate);
    }

    /**
     * Determines whether the current user belongs in a particular sampling bucket. This is
     * determined by taking the last four hex digits of the appInstallID and testing them modulo
     * the sampling rate that is provided.
     *
     * Don't use this method when running to determine whether or not the user falls into a control
     * or test group in any kind of tests (such as A/B tests), as that would introduce sampling
     * biases which would invalidate the test.
     * @return Whether the current user is part of the requested sampling rate bucket.
     */
    @SuppressWarnings("magicnumber")
    @VisibleForTesting
    protected static boolean isUserInSamplingGroup(@NonNull String appInstallID, int sampleRate) {
        try {
            int lastFourDigits = Integer.parseInt(appInstallID.substring(appInstallID.length() - 4), 16);
            return lastFourDigits % sampleRate == 0;
        } catch (Exception e) {
            // Should never happen, but don't crash just in case.
            L.logRemoteError(e);
            return false;
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

    /** @return The session identifier used by {@link #preprocessSessionToken}. */
    @Nullable protected String getSessionToken() {
        return sessionToken;
    }
}
