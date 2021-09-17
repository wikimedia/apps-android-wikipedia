package org.wikipedia.analytics.eventplatform;

import androidx.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.reactivex.rxjava3.schedulers.Schedulers;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_GATEWAY_TIMEOUT;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;
import static org.wikipedia.BuildConfig.META_WIKI_BASE_URI;
import static org.wikipedia.analytics.eventplatform.SamplingConfig.Identifier.DEVICE;
import static org.wikipedia.analytics.eventplatform.SamplingConfig.Identifier.PAGEVIEW;
import static org.wikipedia.analytics.eventplatform.SamplingConfig.Identifier.SESSION;

public final class EventPlatformClient {

    /**
     * Stream configs to be fetched on startup and stored for the duration of the app lifecycle.
     */
    static Map<String, StreamConfig> STREAM_CONFIGS = new HashMap<>();

    /*
     * When ENABLED is false, items can be enqueued but not dequeued.
     * Timers will not be set for enqueued items.
     * QUEUE will not grow beyond MAX_QUEUE_SIZE.
     *
     * Inputs: network connection state on/off, connection state bad y/n?
     * Taken out of iOS client, but flag can be set on the request object to wait until connected to send
     */
    private static boolean ENABLED = WikipediaApp.getInstance().isOnline();

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
        StreamConfig streamConfig = STREAM_CONFIGS.get(event.getStream());
        if (streamConfig == null) {
            return;
        }
        if (!SamplingController.isInSample(event)) {
            return;
        }
        addEventMetadata(event);
        //
        // Temporarily send events immediately in order to investigate discrepancies in
        // the numbers of events submitted in this system vs. legacy eventlogging.
        //
        // https://phabricator.wikimedia.org/T281001
        //
        // OutputBuffer.schedule(event);
        OutputBuffer.sendEventsForStream(streamConfig, Collections.singletonList(event));
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
    static void addEventMetadata(Event event) {
        event.setSessionId(AssociationController.getSessionId());
        event.setAppInstallId(Prefs.INSTANCE.getAppInstallId());
    }

    public static void flushCachedEvents() {
        OutputBuffer.sendAllScheduled();
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

        private static final List<Event> QUEUE = new ArrayList<>();

        /*
         * When an item is added to QUEUE, wait this many ms before sending.
         * If another item is added to QUEUE during this time, reset the countdown.
         */
        private static final int WAIT_MS = 30000;

        private static final int MAX_QUEUE_SIZE = 128;

        private static final Runnable SEND_RUNNABLE = OutputBuffer::sendAllScheduled;

        static synchronized void sendAllScheduled() {
            WikipediaApp.getInstance().getMainThreadHandler().removeCallbacks(SEND_RUNNABLE);

            if (ENABLED) {
                send();
                QUEUE.clear();
            }
        }

        /**
         * Schedule a request to be sent.
         *
         * @param event event data
         */
        static synchronized void schedule(Event event) {
            if (ENABLED || QUEUE.size() <= MAX_QUEUE_SIZE) {
                QUEUE.add(event);
            }

            if (ENABLED) {
                if (QUEUE.size() >= MAX_QUEUE_SIZE) {
                    sendAllScheduled();
                } else {
                    //The arrival of a new item interrupts the timer and resets the countdown.
                    WikipediaApp.getInstance().getMainThreadHandler().removeCallbacks(SEND_RUNNABLE);
                    WikipediaApp.getInstance().getMainThreadHandler().postDelayed(SEND_RUNNABLE, WAIT_MS);
                }
            }
        }

        /**
         * If sending is enabled, attempt to send the provided events.
         * Also batch the events ordered by their streams, as the QUEUE
         * can contain events of different streams
         */
        private static void send() {
            Map<String, ArrayList<Event>> eventsByStream = new HashMap<>();
            for (Event event : QUEUE) {
                String stream = event.getStream();
                if (!eventsByStream.containsKey(stream) || eventsByStream.get(stream) == null) {
                    eventsByStream.put(stream, new ArrayList<>());
                }
                eventsByStream.get(stream).add(event);
            }
            for (String stream : eventsByStream.keySet()) {
                if (Prefs.INSTANCE.isEventLoggingEnabled()) {
                    sendEventsForStream(STREAM_CONFIGS.get(stream), eventsByStream.get(stream));
                }
            }
        }

        private static void sendEventsForStream(@NonNull StreamConfig streamConfig, @NonNull List<Event> events) {
            ServiceFactory.getAnalyticsRest(streamConfig).postEventsHasty(events)
                    .subscribeOn(Schedulers.io())
                    .subscribe(response -> {
                        switch (response.code()) {
                            case HTTP_CREATED: // 201 - Success
                            case HTTP_ACCEPTED: // 202 - Hasty success
                                break;
                            // Status 207 will not be received when sending hastily.
                            // case 207: // Partial Success
                            //    TODO: Retry failed events?
                            //    L.logRemoteError(new RuntimeException(response.toString()));
                            //    break;
                            case HTTP_BAD_REQUEST: // 400 - Failure
                                L.logRemoteError(new RuntimeException(response.toString()));
                                break;
                            // Occasional server errors are unfortunately not unusual, so log the error
                            // but don't crash even on pre-production builds.
                            case HTTP_INTERNAL_ERROR: // 500
                            case HTTP_UNAVAILABLE: // 503
                            case HTTP_GATEWAY_TIMEOUT: // 504
                                L.logRemoteError(new RuntimeException(response.message()));
                                break;
                            default:
                                // Something unexpected happened. Crash if this is a pre-production build.
                                L.logRemoteErrorIfProd(
                                        new RuntimeException("Unexpected EventGate response: "
                                                + response.toString())
                                );
                                break;
                        }
                    }, L::w);
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
                // If there is no runtime value for SESSION_ID, try to load a
                // value from persistent store.
                SESSION_ID = Prefs.INSTANCE.getEventPlatformSessionId();

                if (SESSION_ID == null) {
                    // If there is no value in the persistent store, generate a new value for
                    // SESSION_ID, and write the update to the persistent store.
                    SESSION_ID = generateRandomId();
                    Prefs.INSTANCE.setEventPlatformSessionId(SESSION_ID);
                }
            }
            return SESSION_ID;
        }

        static void beginNewSession() {
            // Clear runtime and persisted value for SESSION_ID.
            SESSION_ID = null;
            Prefs.INSTANCE.setEventPlatformSessionId(null);

            // A session refresh implies a pageview refresh, so clear runtime value of PAGEVIEW_ID.
            PAGEVIEW_ID = null;
        }

        /**
         * @return a string of 20 zero-padded hexadecimal digits representing a uniformly random
         * 80-bit integer
         */
        @SuppressWarnings("checkstyle:magicnumber")
        public static String generateRandomId() {
            Random random = new Random();
            return String.format("%08x", random.nextInt()) + String.format("%08x", random.nextInt()) + String.format("%04x", random.nextInt() & 0xFFFF);
        }
    }

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

            StreamConfig streamConfig = STREAM_CONFIGS.get(stream);

            if (streamConfig == null) {
                return false;
            }

            SamplingConfig samplingConfig = streamConfig.getSamplingConfig();

            if (samplingConfig == null || samplingConfig.getRate() == 1.0) {
                return true;
            }
            if (samplingConfig.getRate() == 0.0) {
                return false;
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
            return (double) Long.parseLong(token, 16) / (double) 0xFFFFFFFFL;
        }

        static String getSamplingId(SamplingConfig.Identifier identifier) {
            if (identifier == SESSION) {
                return AssociationController.getSessionId();
            }
            if (identifier == PAGEVIEW) {
                return AssociationController.getPageViewId();
            }
            if (identifier == DEVICE) {
                return Prefs.INSTANCE.getAppInstallId();
            }
            throw new RuntimeException("Bad identifier type");
        }

    }

    private static void refreshStreamConfigs() {
        ServiceFactory.get(new WikiSite(META_WIKI_BASE_URI))
                .getStreamConfigs()
                .subscribeOn(Schedulers.io())
                .subscribe(response -> updateStreamConfigs(response.getStreamConfigs()), L::e);
    }

    private static synchronized void updateStreamConfigs(@NonNull Map<String, StreamConfig> streamConfigs) {
        STREAM_CONFIGS = streamConfigs;
        Prefs.INSTANCE.setStreamConfigs(STREAM_CONFIGS);
    }

    public static void setUpStreamConfigs() {
        STREAM_CONFIGS = Prefs.INSTANCE.getStreamConfigs();
        refreshStreamConfigs();
    }

    private EventPlatformClient() {
    }
}
