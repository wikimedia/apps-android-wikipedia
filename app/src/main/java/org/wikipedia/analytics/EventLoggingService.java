package org.wikipedia.analytics;

import android.annotation.SuppressLint;
import android.net.Uri;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory;
import org.wikipedia.util.ReleaseUtil;
import org.wikipedia.util.log.L;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.Request;
import okhttp3.RequestBody;

import static org.wikipedia.settings.Prefs.isEventLoggingEnabled;

public final class EventLoggingService {
    private static final RequestBody EMPTY_REQ = RequestBody.create(null, new byte[0]);
    private static final String EVENTLOG_URL_PROD = "https://meta.wikimedia.org/beacon/event";
    private static final String EVENTLOG_URL_DEV = "https://deployment.wikimedia.beta.wmflabs.org/beacon/event";
    private static final String EVENTLOG_URL = ReleaseUtil.isDevRelease()
            ? EVENTLOG_URL_DEV : EVENTLOG_URL_PROD;
    // https://github.com/wikimedia/mediawiki-extensions-EventLogging/blob/8b3cb1b/modules/ext.eventLogging.core.js#L57
    private static final int MAX_URL_LEN = 2000;

    private static EventLoggingService INSTANCE = new EventLoggingService();

    public static EventLoggingService getInstance() {
        return INSTANCE;
    }

    /**
     * Log the current event.
     *
     * Returns immediately after queueing the network request in the background.
     */
    @SuppressLint("CheckResult")
    public void log(JSONObject event) {
        if (!isEventLoggingEnabled() || !WikipediaApp.getInstance().isOnline()) {
            // Do not send events if the user opted out of EventLogging or the device is offline.
            return;
        }

        Completable.fromAction(() -> {
            String eventStr = event.toString();
            String dataURL = Uri.parse(EVENTLOG_URL)
                    .buildUpon().query(eventStr)
                    .build().toString();

            if (ReleaseUtil.isDevRelease()) {
                L.d(eventStr);
            }

            if (dataURL.length() > MAX_URL_LEN) {
                L.logRemoteErrorIfProd(new RuntimeException("EventLogging max length exceeded"));
            }

            Request request = new Request.Builder().url(dataURL).post(EMPTY_REQ).build();
            OkHttpConnectionFactory.getClient().newCall(request).execute().close();
        }).subscribeOn(Schedulers.io())
                .subscribe(() -> { },
                        throwable -> L.d("Lost EL data: " + event.toString(), throwable));
    }

    private EventLoggingService() { }
}
