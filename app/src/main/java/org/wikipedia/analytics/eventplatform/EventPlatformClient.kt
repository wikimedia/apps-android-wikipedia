package org.wikipedia.analytics.eventplatform

import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.BuildConfig
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil
import org.wikipedia.util.log.L
import java.net.HttpURLConnection
import java.util.*

object EventPlatformClient {
    /**
     * Stream configs to be fetched on startup and stored for the duration of the app lifecycle.
     */
    private val STREAM_CONFIGS = mutableMapOf<String, StreamConfig>()

    /*
     * When ENABLED is false, items can be enqueued but not dequeued.
     * Timers will not be set for enqueued items.
     * QUEUE will not grow beyond MAX_QUEUE_SIZE.
     *
     * Inputs: network connection state on/off, connection state bad y/n?
     * Taken out of iOS client, but flag can be set on the request object to wait until connected to send
     */
    private var ENABLED = WikipediaApp.getInstance().isOnline

    fun setStreamConfig(streamConfig: StreamConfig) {
        STREAM_CONFIGS[streamConfig.streamName] = streamConfig
    }

    fun getStreamConfig(name: String): StreamConfig? {
        return STREAM_CONFIGS[name]
    }

    /**
     * Set whether the client is enabled. This can react to device online/offline state as well
     * as other considerations.
     */
    @Synchronized
    fun setEnabled(enabled: Boolean) {
        ENABLED = enabled
        if (ENABLED) {
            /*
             * Try immediately to send any enqueued items. Otherwise another
             * item must be enqueued before sending is triggered.
             */
            OutputBuffer.sendAllScheduled()
        }
    }

    /**
     * Submit an event to be enqueued and sent to the Event Platform
     *
     * @param event event
     */
    @Synchronized
    fun submit(event: Event) {
        if (!SamplingController.isInSample(event)) {
            return
        }
        addEventMetadata(event)
        OutputBuffer.schedule(event)
    }

    /**
     * Supplement the outgoing event with additional metadata, if not already present.
     * These include:
     * - dt: ISO 8601 timestamp
     * - app_session_id: the current session ID
     * - app_install_id: app install ID
     *
     * @param event event
     */
    fun addEventMetadata(event: Event) {
        if (event is MobileAppsEvent) {
            event.sessionId = AssociationController.sessionId
            event.appInstallId = Prefs.appInstallId
        }
        event.dt = DateUtil.iso8601DateFormat(Date())
    }

    fun flushCachedEvents() {
        OutputBuffer.sendAllScheduled()
    }

    private fun refreshStreamConfigs() {
        ServiceFactory.get(WikiSite(BuildConfig.META_WIKI_BASE_URI)).streamConfigs
                .subscribeOn(Schedulers.io())
                .subscribe({ updateStreamConfigs(it.streamConfigs) }) { L.e(it) }
    }

    @Synchronized
    private fun updateStreamConfigs(streamConfigs: Map<String, StreamConfig>) {
        STREAM_CONFIGS.clear()
        STREAM_CONFIGS.putAll(streamConfigs)
        Prefs.streamConfigs = STREAM_CONFIGS
    }

    fun setUpStreamConfigs() {
        STREAM_CONFIGS.clear()
        STREAM_CONFIGS.putAll(Prefs.streamConfigs)
        refreshStreamConfigs()
    }

    /**
     * OutputBuffer: buffers events in a queue prior to transmission
     *
     * Transmissions are not sent at a uniform offset but are shaped into
     * 'bursts' using a combination of queue size and debounce time.
     *
     * These concentrate requests (and hence, theoretically, radio awake state)
     * so as not to contribute to battery drain.
     */
    internal object OutputBuffer {
        private val QUEUE = mutableListOf<Event>()

        /*
         * When an item is added to QUEUE, wait this many ms before sending.
         * If another item is added to QUEUE during this time, reset the countdown.
         */
        private const val WAIT_MS = 30000
        private const val MAX_QUEUE_SIZE = 64
        private val SEND_RUNNABLE = Runnable { sendAllScheduled() }

        @Synchronized
        fun sendAllScheduled() {
            WikipediaApp.getInstance().mainThreadHandler.removeCallbacks(SEND_RUNNABLE)
            if (ENABLED) {
                send()
                QUEUE.clear()
            }
        }

        /**
         * Schedule a request to be sent.
         *
         * @param event event data
         */
        @Synchronized
        fun schedule(event: Event) {
            if (ENABLED || QUEUE.size <= MAX_QUEUE_SIZE) {
                QUEUE.add(event)
            }
            if (ENABLED) {
                if (QUEUE.size >= MAX_QUEUE_SIZE) {
                    sendAllScheduled()
                } else {
                    // The arrival of a new item interrupts the timer and resets the countdown.
                    WikipediaApp.getInstance().mainThreadHandler.removeCallbacks(SEND_RUNNABLE)
                    WikipediaApp.getInstance().mainThreadHandler.postDelayed(SEND_RUNNABLE, WAIT_MS.toLong())
                }
            }
        }

        /**
         * If sending is enabled, attempt to send the provided events.
         * Also batch the events ordered by their streams, as the QUEUE
         * can contain events of different streams
         */
        private fun send() {
            if (!Prefs.isEventLoggingEnabled) {
                return
            }
            QUEUE.groupBy { it.stream }.forEach { (stream, events) ->
                sendEventsForStream(STREAM_CONFIGS[stream]!!, events)
            }
        }

        fun sendEventsForStream(streamConfig: StreamConfig, events: List<Event>) {
            ServiceFactory.getAnalyticsRest(streamConfig).postEventsHasty(events)
                    .subscribeOn(Schedulers.io())
                    .subscribe({
                        when (it.code()) {
                            HttpURLConnection.HTTP_CREATED,
                            HttpURLConnection.HTTP_ACCEPTED -> {}
                            else -> {
                                // Received successful response, but unexpected HTTP code.
                                // TODO: queue up to retry?
                            }
                        }
                    }) {
                        if (it is HttpStatusException) {
                            when (it.code) {
                                HttpURLConnection.HTTP_BAD_REQUEST,
                                HttpURLConnection.HTTP_INTERNAL_ERROR,
                                HttpURLConnection.HTTP_UNAVAILABLE,
                                HttpURLConnection.HTTP_GATEWAY_TIMEOUT -> {
                                    L.e(it)
                                    // TODO: queue up to retry?
                                }
                                else -> {
                                    // Something unexpected happened. Crash if this is a pre-production build.
                                    L.logRemoteErrorIfProd(it)
                                }
                            }
                        } else {
                            L.w(it)
                        }
                    }
        }
    }

    /**
     * AssociationController: provides associative identifiers and manage their
     * persistence
     *
     * Identifiers correspond to various scopes e.g. 'pageview', 'session', and 'device'.
     *
     * TODO: Possibly get rid of the pageview type?  Does it make sense on apps?  It is not in the iOS library currently.
     * On apps, a "session" starts when the app is loaded, and ends when completely closed, or after 15 minutes of inactivity
     * Save a ts when going into bg, then when returning to foreground, & if it's been more than 15 mins, start a new session, else continue session from before
     * Possible to query/track time since last interaction? (For future)
     */
    internal object AssociationController {
        private var PAGEVIEW_ID: String? = null
        private var SESSION_ID: String? = null

        /**
         * Generate a pageview identifier.
         *
         * @return pageview ID
         *
         * The identifier is a string of 20 zero-padded hexadecimal digits
         * representing a uniformly random 80-bit integer.
         */
        val pageViewId: String
            get() {
                if (PAGEVIEW_ID == null) {
                    PAGEVIEW_ID = generateRandomId()
                }
                return PAGEVIEW_ID!!
            }

        /**
         * Generate a session identifier.
         *
         * @return session ID
         *
         * The identifier is a string of 20 zero-padded hexadecimal digits
         * representing a uniformly random 80-bit integer.
         */
        val sessionId: String
            get() {
                if (SESSION_ID == null) {
                    // If there is no runtime value for SESSION_ID, try to load a
                    // value from persistent store.
                    SESSION_ID = Prefs.eventPlatformSessionId
                    if (SESSION_ID == null) {
                        // If there is no value in the persistent store, generate a new value for
                        // SESSION_ID, and write the update to the persistent store.
                        SESSION_ID = generateRandomId()
                        Prefs.eventPlatformSessionId = SESSION_ID
                    }
                }
                return SESSION_ID!!
            }

        fun beginNewSession() {
            // Clear runtime and persisted value for SESSION_ID.
            SESSION_ID = null
            Prefs.eventPlatformSessionId = null

            // A session refresh implies a pageview refresh, so clear runtime value of PAGEVIEW_ID.
            PAGEVIEW_ID = null
        }

        /**
         * @return a string of 20 zero-padded hexadecimal digits representing a uniformly random
         * 80-bit integer
         */
        private fun generateRandomId(): String {
            val random = Random()
            return String.format("%08x", random.nextInt()) + String.format("%08x", random.nextInt()) + String.format("%04x", random.nextInt() and 0xFFFF)
        }
    }

    /**
     * SamplingController: computes various sampling functions on the client
     *
     * Sampling is based on associative identifiers, each of which have a
     * well-defined scope, and sampling config, which each stream provides as
     * part of its configuration.
     */
    internal object SamplingController {
        private var SAMPLING_CACHE = mutableMapOf<String, Boolean>()

        /**
         * @param event event
         * @return true if in sample or false otherwise
         */
        fun isInSample(event: Event): Boolean {
            val stream = event.stream
            if (SAMPLING_CACHE.containsKey(stream)) {
                return SAMPLING_CACHE[stream]!!
            }
            val streamConfig = STREAM_CONFIGS[stream] ?: return false
            val samplingConfig = streamConfig.samplingConfig
            if (samplingConfig == null || samplingConfig.rate == 1.0) {
                return true
            }
            if (samplingConfig.rate == 0.0) {
                return false
            }
            val inSample = getSamplingValue(samplingConfig.getIdentifier()) < samplingConfig.rate
            SAMPLING_CACHE[stream] = inSample
            return inSample
        }

        /**
         * @param identifier identifier type from sampling config
         * @return a floating point value between 0.0 and 1.0 (inclusive)
         */
        fun getSamplingValue(identifier: SamplingConfig.Identifier): Double {
            val token = getSamplingId(identifier).substring(0, 8)
            return token.toLong(16).toDouble() / 0xFFFFFFFFL.toDouble()
        }

        fun getSamplingId(identifier: SamplingConfig.Identifier): String {
            if (identifier === SamplingConfig.Identifier.SESSION) {
                return AssociationController.sessionId
            }
            if (identifier === SamplingConfig.Identifier.PAGEVIEW) {
                return AssociationController.pageViewId
            }
            if (identifier === SamplingConfig.Identifier.DEVICE) {
                return Prefs.appInstallId.orEmpty()
            }
            throw RuntimeException("Bad identifier type")
        }
    }
}
