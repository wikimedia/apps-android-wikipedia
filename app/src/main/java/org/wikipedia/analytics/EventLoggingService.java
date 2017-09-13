package org.wikipedia.analytics;

import android.net.Uri;

import org.json.JSONObject;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.crash.RemoteLogException;
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory;
import org.wikipedia.util.ReleaseUtil;
import org.wikipedia.util.log.L;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static org.wikipedia.settings.Prefs.isEventLoggingEnabled;

public final class EventLoggingService {
    private static final RequestBody EMPTY_REQ = RequestBody.create(null, new byte[0]);
    private static final String EVENTLOG_URL_PROD = "https://meta.wikimedia.org/beacon/event";
    private static final String EVENTLOG_URL_DEV = "https://deployment.wikimedia.beta.wmflabs.org/beacon/event";
    private static final String EVENTLOG_URL = ReleaseUtil.isPreBetaRelease()
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
    public void log(JSONObject event) {
        if (!isEventLoggingEnabled()) {
            // Do not send events if the user opted out of EventLogging
            return;
        }

        new LogEventTask(event).execute();
    }

    private class LogEventTask extends SaneAsyncTask<Integer> {
        private final JSONObject data;

        LogEventTask(JSONObject data) {
            this.data = data;
        }

        @Override
        public Integer performTask() throws Throwable {
            String dataURL = Uri.parse(EVENTLOG_URL)
                    .buildUpon().query(data.toString())
                    .build().toString();

            if (dataURL.length() > MAX_URL_LEN) {
                L.logRemoteErrorIfProd(new RemoteLogException("EventLogging max length exceeded")
                        .put("length", String.valueOf(dataURL.length())));
            }

            Request request = new Request.Builder().url(dataURL).post(EMPTY_REQ).build();
            Response response = OkHttpConnectionFactory.getClient().newCall(request).execute();
            try {
                return response.code();
            } finally {
                response.close();
            }
        }

        @Override
        public void onCatch(Throwable caught) {
            // Do nothing bad. EL data is ok to lose.
            L.d("Lost EL data: " + data.toString());
        }
    }

    private EventLoggingService() { }
}
