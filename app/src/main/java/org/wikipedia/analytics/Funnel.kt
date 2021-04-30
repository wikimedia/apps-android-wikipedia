package org.wikipedia.analytics

import androidx.annotation.VisibleForTesting
import com.google.gson.annotations.SerializedName
import org.json.JSONException
import org.json.JSONObject
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.util.DateUtil.iso8601LocalDateFormat
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.util.log.L
import java.util.*

/** Schemas for this abstract funnel are expected to have appInstallID and sessionToken fields. When
 * these fields are not present or differently named, preprocess* or get*Field should be overridden.  */
abstract class Funnel @JvmOverloads internal constructor(protected val app: WikipediaApp, private val schemaName: String,
                                                         private val revision: Int, private val sampleRate: Int = SAMPLE_LOG_ALL,
        // todo: remove @SerializedName if not pickled
                                                         @field:SerializedName("site") private val wiki: WikiSite? = null) {
    private val sampleRateRemoteParamName: String = schemaName + "_rate"

    /** @return The session identifier used by [.preprocessSessionToken].
     */
    val sessionToken = UUID.randomUUID().toString()

    /*package*/
    internal constructor(app: WikipediaApp, schemaName: String, revision: Int, wiki: WikiSite?) :
            this(app, schemaName, revision, SAMPLE_LOG_ALL, wiki) {
    }

    /**
     * Optionally pre-process the event data before sending to EL.
     *
     * @param eventData Event Data so far collected
     * @return Event Data to be sent to server
     */
    protected open fun preprocessData(eventData: JSONObject): JSONObject? {
        preprocessData(eventData, DEFAULT_TIMESTAMP_KEY, iso8601LocalDateFormat(Date()))
        preprocessData(eventData, DEFAULT_APP_INSTALL_ID_KEY, app.appInstallID)
        preprocessSessionToken(eventData)
        return eventData
    }

    /** Invokes [JSONObject.put] on `data` and throws a [RuntimeException] on
     * failure.  */
    protected fun <T> preprocessData(eventData: JSONObject, key: String, `val`: T) {
        try {
            eventData.put(key, `val`)
        } catch (e: JSONException) {
            throw RuntimeException("key=$key val=$`val`", e)
        }
    }

    /** Invoked by [.preprocessData].  */
    protected open fun preprocessSessionToken(eventData: JSONObject) {
        preprocessData<String?>(eventData, DEFAULT_SESSION_TOKEN_KEY, sessionToken)
    }

    protected fun log(vararg params: Any?) {
        log(wiki, *params)
    }

    /**
     * Logs an event.
     *
     * @param params        Actual data for the event. Considered to be an array
     * of alternating key and value items (for easier
     * use in subclass constructors).
     *
     * For example, what would be expressed in a more sane
     * language as:
     *
     * .log({
     * "page": "List of mass murderers",
     * "section": "2014"
     * });
     *
     * would be expressed here as
     *
     * .log(
     * "page", "List of mass murderers",
     * "section", "2014"
     * );
     *
     * This format should be only used in subclass methods directly.
     * The subclass methods should take more explicit parameters
     * depending on what they are logging.
     */
    protected fun log(wiki: WikiSite?, vararg params: Any) {
        if (ReleaseUtil.isDevRelease || isUserInSamplingGroup(app.appInstallID, getSampleRate())) {
            val eventData = JSONObject()
            var i = 0
            while (i < params.size) {
                preprocessData(eventData, params[i].toString(), params[i + 1])
                i += 2
            }
            val event = EventLoggingEvent(
                    schemaName,
                    revision,
                    wiki?.dbName() ?: app.wikiSite.dbName(),
                    preprocessData(eventData)
            )
            EventLoggingService.instance.log(event.data)
        }
    }

    /**
     * @return Sampling rate for this funnel, as given by the remote config parameter for this
     * funnel (the name of which is defined as "[schema name]_rate"), with a fallback to the
     * hard-coded sampling rate passed into the constructor.
     */
    private fun getSampleRate(): Int {
        return app.remoteConfig.config.optInt(sampleRateRemoteParamName, sampleRate)
    }

    companion object {
        const val SAMPLE_LOG_1K = 1000
        const val SAMPLE_LOG_100 = 100
        const val SAMPLE_LOG_10 = 10
        const val SAMPLE_LOG_ALL = 1
        private const val DEFAULT_TIMESTAMP_KEY = "client_dt"
        private const val DEFAULT_APP_INSTALL_ID_KEY = "app_install_id"
        private const val DEFAULT_SESSION_TOKEN_KEY = "session_token"

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
        @kotlin.jvm.JvmStatic
        @VisibleForTesting
        fun isUserInSamplingGroup(appInstallID: String?, sampleRate: Int): Boolean {
            return try {
                val lastFourDigits = appInstallID!!.substring(appInstallID.length - 4).toInt(16)
                lastFourDigits % sampleRate == 0
            } catch (e: Exception) {
                // Should never happen, but don't crash just in case.
                L.logRemoteError(e)
                false
            }
        }
    }
}
