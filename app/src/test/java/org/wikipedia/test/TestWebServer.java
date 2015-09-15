package org.wikipedia.test;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.wikipedia.testlib.TestConstants;

import java.io.IOException;
import java.net.URL;

public class TestWebServer {
    private final MockWebServer server;

    public TestWebServer() {
        server = new MockWebServer();
    }

    public void setUp() throws IOException {
        server.start();
    }

    public void tearDown() throws IOException {
        server.shutdown();
    }

    public URL getUrl() {
        return getUrl("");
    }

    public URL getUrl(String path) {
        return server.getUrl(path);
    }

    public int getRequestCount() {
        return server.getRequestCount();
    }

    public void enqueue(String body) {
        enqueue(new MockResponse().setBody(body));
    }

    public void enqueue(MockResponse response) {
        server.enqueue(response);
    }

    public void takeRequest() throws InterruptedException {
        if (server.takeRequest(TestConstants.TIMEOUT_DURATION, TestConstants.TIMEOUT_UNIT) == null) {
            throw new InterruptedException("Timeout elapsed.");
        }
    }
}