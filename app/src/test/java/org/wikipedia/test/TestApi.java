package org.wikipedia.test;

import android.support.annotation.NonNull;

import org.mediawiki.api.json.Api;

import java.net.MalformedURLException;
import java.net.URL;

public class TestApi extends Api {
    @NonNull
    private final TestWebServer server;

    public TestApi(@NonNull TestWebServer server) {
        super("domain");
        this.server =  server;
    }

    @Override
    public URL getApiUrl() {
        try {
            return new URL(server.getUrl("/"));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}