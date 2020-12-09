package org.wikipedia.analytics.eventplatform;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static org.apache.commons.lang3.StringUtils.leftPad;
import static org.wikipedia.analytics.eventplatform.EventPlatformClientIntegration.deleteStoredSessionId;
import static org.wikipedia.analytics.eventplatform.EventPlatformClientIntegration.fetchStreamConfigs;
import static org.wikipedia.analytics.eventplatform.EventPlatformClientIntegration.getAppInstallId;
import static org.wikipedia.analytics.eventplatform.EventPlatformClientIntegration.getIso8601Timestamp;
import static org.wikipedia.analytics.eventplatform.EventPlatformClientIntegration.getStoredSessionId;
import static org.wikipedia.analytics.eventplatform.EventPlatformClientIntegration.getStoredStreamConfigs;
import static org.wikipedia.analytics.eventplatform.EventPlatformClientIntegration.isOnline;
import static org.wikipedia.analytics.eventplatform.EventPlatformClientIntegration.postEvent;
import static org.wikipedia.analytics.eventplatform.EventPlatformClientIntegration.setStoredSessionId;
import static org.wikipedia.analytics.eventplatform.EventPlatformClientIntegration.setStoredStreamConfigs;
import static org.wikipedia.analytics.eventplatform.SamplingConfig.Identifier.DEVICE;
import static org.wikipedia.analytics.eventplatform.SamplingConfig.Identifier.PAGEVIEW;
import static org.wikipedia.analytics.eventplatform.SamplingConfig.Identifier.SESSION;

public final class EventPlatformClient {

    /**
     * Stream configs to be fetched on startup and stored for the duration of the application
     * lifecycle.
     */
    static Map<String, StreamConfig> STREAM_CONFIGS = new HashMap<>();

    /*
     * When ENABLED is false, items can be enqueued but not dequeued.
     * Timers will not be set for enqueued items.
     * QUEUE may grow beyond WAIT_ITEMS.
     *
     * Inputs: network connection state on/off, connection state bad y/n?
     * Taken out of iOS client, but flag can be set on the request object to wait until connected to send
     */
    private static boolean ENABLED = isOnline();


    // A regular expression to match JavaScript regular expression literals. (How meta!)
    // This is not as strict as it could be in that it allows individual flags to be specified more
    // than once, but it doesn't really matter because we don't expect flags and ignore them if
    // present.
    static String JS_REGEXP_PATTERN = "^/.*/[gimsuy]{0,6}$";

    /**
     * Get the stream config for a given stream name.
     *
     * Stream configuration keys can take the form of either a string literal or a (JavaScript)
     * regular expression pattern. To take advantage of the performance strengths of HashMaps, we'll
     * first attempt to retrieve the stream config as a literal key name. If no match is found,
     * we'll then iterate over the keys to search for a regular expression matching the provided
     * stream name.
     *
     * Regex-formatted keys use JavaScript regular expression literal syntax, e.g.: '/foo/'.
     * We don't expect any flags, and ignore them if present.
     *
     * N.B. Since stream config keys can be given as regular expressions, it is technically
     * possible that more than one key could match the provided stream name. In this event that more
     * than one match is present, we'll return the config corresponding to the first match found.
     *
     * @param streamName stream name
     * @return the first matching stream config, or null if no match is found
     */
    static StreamConfig getStreamConfig(String streamName) {
        if (STREAM_CONFIGS.containsKey(streamName)) {
            return STREAM_CONFIGS.get(streamName);
        }

        for (String key : STREAM_CONFIGS.keySet()) {
            if (key.matches(JS_REGEXP_PATTERN)) {
                // Note: After splitting on the slash character ("/"), element [0] of the resulting
                // array will contain the empty string (""), and element [1] will contain the
                // regular expression pattern.
                // If any flags are specified, they will be present in element [2], but will be
                // ignored here.
                if (streamName.matches(key.split("/")[1])) {
                    return STREAM_CONFIGS.get(key);
                }
            }
        }

        return null;
    }

    static void setStreamConfig(StreamConfig streamConfig) {
        STREAM_CONFIGS.put(streamConfig.getStreamName(), streamConfig);
    }

    /**
     * Set whether the client is enabled. This can react to device online/offline state as well
     * as other considerations.
     */
    public static synchronized void setEnabled(boolean enabled) {
        ENABLED = enabled;

        if (ENABLED) {
            /*
             * Try immediately to send any enqueued items. Otherwise another
             * item must be enqueued before sending is triggered.
             */
            OutputBuffer.sendAllScheduled();
        }
    }

    /**
     * Submit an event to be enqueued and sent to the Event Platform
     *
     * @param event event
     */
    public static synchronized void submit(Event event) {
        if (!SamplingController.isInSample(event)) {
            return;
        }
        addEventMetadata(event);
        OutputBuffer.schedule(event);
    }

    /**
     * Supplement the outgoing event with additional metadata, if not already present.
     * These include:
     * - client_dt: ISO 8601 timestamp
     * - app_session_id: the current session ID
     * - app_install_id: app install ID
     *
     * @param event event
     */
    static void addEventMetadata(Event event) {
        if (event.getTimestamp() == null) {
            event.setTimestamp(getIso8601Timestamp());
        }
        event.setSessionId(AssociationController.getSessionId());
        event.setAppInstallId(getAppInstallId());
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
    static class OutputBuffer {

        private static List<Event> QUEUE = new ArrayList<>();

        /*
         * When an item is added to QUEUE, wait this many ms before sending.
         *
         * If another item is added to QUEUE during this time, reset the
         * countdown.
         */
        private static final int WAIT_MS = 30000;

        /*
         * When QUEUE.size() exceeds this value TIMER becomes non-interruptable.
         */
        private static final int MAX_QUEUE_SIZE = 128;

        /*
         * IMPLEMENTATION NOTE: Java Timer will provide the desired asynchronous
         * countdown after a new item is added to QUEUE.
         */
        private static Timer TIMER = new Timer();

        /*
         * IMPLEMENTATION NOTE: Java abstract TimerTask class requires a run()
         * method be defined.
         *
         * The run() method is called when the Timer expires.
         */
        private static class Task extends TimerTask {
            public void run() {
                sendAllScheduled();
            }
        }

        /**
         * If sending is enabled, dequeue and call send() on all scheduled items.
         */
        static synchronized void sendAllScheduled() {
            TIMER.cancel();

            if (ENABLED) {
                /*
                 * All items on QUEUE are permanently removed.
                 */
                for (Event event : QUEUE) {
                    send(event);
                }
                QUEUE = new ArrayList<>();
            }
        }

        /**
         * Schedule a request to be sent.
         *
         * @param event event data
         */
        static synchronized void schedule(Event event) {
            /*
             * Item is enqueued whether or not sending is enabled.
             */
            QUEUE.add(event);

            if (ENABLED) {
                if (QUEUE.size() >= MAX_QUEUE_SIZE) {
                    /*
                     * >= because while sending is disabled, any number of items
                     * could be added to QUEUE without it emptying.
                     */
                    sendAllScheduled();
                } else {
                    /*
                     * The arrival of a new item interrupts the timer and resets
                     * the countdown.
                     */
                    TIMER.cancel();
                    TIMER.purge();
                    TIMER = new Timer();
                    TIMER.schedule(new Task(), WAIT_MS);
                }
            }
        }

        /**
         * If sending is enabled, attempt to send the provided event.
         *
         * @param event event
         */
        static void send(Event event) {
            postEvent(getStreamConfig(event.getStream()), event);
        }

    }

    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */

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
    static class AssociationController {
        private static String PAGEVIEW_ID = null;
        private static String SESSION_ID = null;

        /**
         * Generate a pageview identifier.
         *
         * @return pageview ID
         *
         * The identifier is a string of 20 zero-padded hexadecimal digits
         * representing a uniformly random 80-bit integer.
         */
        static String getPageViewId() {
            if (PAGEVIEW_ID == null) {
                PAGEVIEW_ID = generateRandomId();
            }
            return PAGEVIEW_ID;
        }

        /**
         * Generate a session identifier.
         *
         * @return session ID
         *
         * The identifier is a string of 20 zero-padded hexadecimal digits
         * representing a uniformly random 80-bit integer.
         */
        static String getSessionId() {
            if (SESSION_ID == null) {
                /*
                 * If there is no runtime value for SESSION_ID, try to load a
                 * value from persistent store.
                 */
                SESSION_ID = getStoredSessionId();

                if (SESSION_ID == null) {
                    /*
                     * If there is no value in the persistent store, generate a
                     * new value for SESSION_ID, and write the update to the
                     * persistent store.
                     */
                    SESSION_ID = generateRandomId();
                    setStoredSessionId(SESSION_ID);
                }
            }
            return SESSION_ID;
        }

        /**
         * Unset the session.
         */
        static void beginNewSession() {
            /*
             * Clear runtime and persisted value for SESSION_ID.
             */
            SESSION_ID = null;
            deleteStoredSessionId();

            /*
             * A session refresh implies a pageview refresh, so clear runtime
             * value of PAGEVIEW_ID.
             */
            PAGEVIEW_ID = null;
        }

        /**
         * @return a string of 20 zero-padded hexadecimal digits representing a uniformly random
         * 80-bit integer
         */
        @SuppressWarnings("checkstyle:magicnumber")
        static String generateRandomId() {
            SecureRandom rand = new SecureRandom();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                String chunk = leftPad(Integer.toString(rand.nextInt(65535), 16), 4, '0');
                builder.append(chunk);
            }
            return builder.toString();
        }
    }

    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */

    /**
     * SamplingController: computes various sampling functions on the client
     *
     * Sampling is based on associative identifiers, each of which have a
     * well-defined scope, and sampling config, which each stream provides as
     * part of its configuration.
     */
    static class SamplingController {

        static Map<String, Boolean> SAMPLING_CACHE = new HashMap<>();

        /**
         * @param event event
         * @return true if in sample or false otherwise
         */
        static boolean isInSample(Event event) {
            String stream = event.getStream();

            if (SAMPLING_CACHE.containsKey(stream)) {
                return SAMPLING_CACHE.get(stream);
            }

            StreamConfig streamConfig = getStreamConfig(stream);

            /*
             * If the specified stream isn't configured, bail out.
             */
            if (streamConfig == null) {
                return false;
            }

            SamplingConfig samplingConfig = streamConfig.getSamplingConfig();

            /*
             * Default to 100% (always in-sample) for this stream if the stream is configured
             * but has no sampling config defined.
             */
            if (samplingConfig == null) {
                return true;
            }

            /*
             * Take a shortcut if the sampling rate is zero or one.
             */
            if (samplingConfig.getRate() == 0.0) {
                return false;
            }
            if (samplingConfig.getRate() == 1.0) {
                return true;
            }

            boolean inSample = getSamplingValue(samplingConfig.getIdentifier()) < samplingConfig.getRate();
            SAMPLING_CACHE.put(stream, inSample);

            return inSample;
        }

        /**
         * @param identifier identifier type from sampling config
         * @return a floating point value between 0.0 and 1.0 (inclusive)
         */
        @SuppressWarnings("checkstyle:magicnumber")
        static double getSamplingValue(SamplingConfig.Identifier identifier) {
            String token = getSamplingId(identifier).substring(0, 8);
            return (double) Long.parseLong(token, 16) / (double) Long.parseLong("ffffffff", 16);
        }

        static String getSamplingId(SamplingConfig.Identifier identifier) {
            if (identifier == SESSION) {
                return AssociationController.getSessionId();
            }
            if (identifier == PAGEVIEW) {
                return AssociationController.getPageViewId();
            }
            if (identifier == DEVICE) {
                return getAppInstallId();
            }
            throw new RuntimeException("Bad identifier type");
        }

    }

    interface StreamConfigsCallback {
        void onSuccess(Map<String, StreamConfig> streamConfigs);
    }

    private static synchronized void refreshStreamConfigs() {
        fetchStreamConfigs(streamConfigs -> {
            STREAM_CONFIGS = streamConfigs;
            setStoredStreamConfigs(streamConfigs);
        });
    }

    /*
     * The constructor is private, so instantiation from other classes is impossible.
     */
    public static void setUpStreamConfigs() {
        STREAM_CONFIGS = getStoredStreamConfigs();
        refreshStreamConfigs();
    }

    private EventPlatformClient() {
    }
}
