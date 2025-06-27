package org.wikimedia.metrics_platform.config;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class StreamConfigFetcher {
    public static final String ANALYTICS_API_ENDPOINT = "https://meta.wikimedia.org/w/api.php?" +
            "action=streamconfigs&format=json&formatversion=2&" +
            "constraints=destination_event_service%3Deventgate-analytics-external";

    public static final String METRICS_PLATFORM_SCHEMA_TITLE = "analytics/mediawiki/client/metrics_event";

    private final URL url;
    private final OkHttpClient httpClient;
    private final Gson gson;

    public StreamConfigFetcher(URL url, OkHttpClient httpClient, Gson gson) {
        this.url = url;
        this.httpClient = httpClient;
        this.gson = gson;
    }

    /**
     * Fetch stream configs from analytics endpoint.
     */
    public SourceConfig fetchStreamConfigs() throws IOException {
        Request request = new Request.Builder().url(url).build();
        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Failed to fetch stream configs: " + response.message());
            }
            return new SourceConfig(parseConfig(body.charStream()));
        }
    }

    // Visible For Testing
    public Map<String, StreamConfig> parseConfig(Reader reader) {
        return gson.fromJson(reader, StreamConfigCollection.class).streamConfigs.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
