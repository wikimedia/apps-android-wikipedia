package org.wikimedia.metrics_platform;

import static java.util.logging.Level.INFO;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import org.wikimedia.metrics_platform.event.EventProcessed;

import com.google.gson.Gson;

import lombok.extern.java.Log;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Log
public class EventSenderDefault implements EventSender {

    private final Gson gson;
    private final OkHttpClient httpClient;

    public EventSenderDefault(Gson gson, OkHttpClient httpClient) {
        this.gson = gson;
        this.httpClient = httpClient;
    }

    @Override
    public void sendEvents(URL baseUri, Collection<EventProcessed> events) throws IOException {
        Request request = new Request.Builder()
            .url(baseUri)
            .header("Accept", "application/json")
            .header("User-Agent", "Metrics Platform Client/Java " + MetricsClient.METRICS_PLATFORM_LIBRARY_VERSION)
            .post(RequestBody.create(gson.toJson(events), okhttp3.MediaType.parse("application/json")))
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            int status = response.code();
            ResponseBody body = response.body();
            if (!response.isSuccessful() || status == 207) {
                // In the case of a multi-status response (207), it likely means that one or more
                // events were rejected. In such a case, the error is actually contained in
                // the normal response body.
                throw new IOException(body.string());
            }

            log.log(INFO, "Sent " + events.size() + " events successfully.");
        }
    }
}
