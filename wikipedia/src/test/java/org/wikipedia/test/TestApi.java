package org.wikipedia.test;

import android.support.annotation.NonNull;

import org.mediawiki.api.json.Api;

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
        return server.getUrl("/");
    }
}