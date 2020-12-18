package org.wikipedia.analytics.eventplatform;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.log.L;

import java.util.Date;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_GATEWAY_TIMEOUT;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;
import static org.wikipedia.BuildConfig.META_WIKI_BASE_URI;
import static org.wikipedia.settings.Prefs.getEventPlatformSessionId;
import static org.wikipedia.settings.Prefs.getStreamConfigs;
import static org.wikipedia.settings.Prefs.setEventPlatformSessionId;
import static org.wikipedia.settings.Prefs.setStreamConfigs;
import static org.wikipedia.util.DateUtil.iso8601DateFormat;

final class EventPlatformClientIntegration {

    static void fetchStreamConfigs(EventPlatformClient.StreamConfigsCallback cb) {
        CompositeDisposable DISPOSABLES = new CompositeDisposable();
        DISPOSABLES.add(ServiceFactory.get(new WikiSite(META_WIKI_BASE_URI))
                .getStreamConfigs()
                .doAfterTerminate(DISPOSABLES::clear)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> cb.onSuccess(response.getStreamConfigs()), L::e));
    }

    /**
     * Post events to EventGate.
     *
     * Failures are logged remotely (as well as in Logcat). The only exception is for unexpected
     * responses, for which we crash on pre-production builds.
     *
     * @param streamConfig stream config
     * @param events Events to be posted. Gson will take care of serializing to JSON.
     */
    static void postEvents(@NonNull StreamConfig streamConfig, @NonNull List<Event> events) {
        CompositeDisposable DISPOSABLES = new CompositeDisposable();
        DISPOSABLES.add(ServiceFactory.getAnalyticsRest(streamConfig).postEvents(events)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate(DISPOSABLES::clear)
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
                }));
    }

    /**
     * Get a persistent event platform session ID from the application preferences.
     *
     * @return session ID
     */
    @Nullable
    static String getStoredSessionId() {
        return getEventPlatformSessionId();
    }

    /**
     * Set a persistent event platform session ID in the application preferences.
     *
     * @param sessionId session ID
     */
    static void setStoredSessionId(@NonNull String sessionId) {
        setEventPlatformSessionId(sessionId);
    }

    /**
     * Delete a persistent event platform session ID from the application preferences.
     */
    static void deleteStoredSessionId() {
        setEventPlatformSessionId(null);
    }

    @NonNull
    static Map<String, StreamConfig> getStoredStreamConfigs() {
        return getStreamConfigs();
    }

    static void setStoredStreamConfigs(@NonNull Map<String, StreamConfig> streamConfigs) {
        setStreamConfigs(streamConfigs);
    }

    @NonNull
    static String getIso8601Timestamp() {
        return iso8601DateFormat(new Date());
    }

    @Nullable
    static String getAppInstallId() {
        return Prefs.getAppInstallId();
    }

    static boolean isOnline() {
        return WikipediaApp.getInstance().isOnline();
    }

    private EventPlatformClientIntegration() {
    }

}
