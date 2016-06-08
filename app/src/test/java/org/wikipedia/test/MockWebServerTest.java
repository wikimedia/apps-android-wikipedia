package org.wikipedia.test;

import android.support.annotation.NonNull;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

@RunWith(TestRunner.class)
public abstract class MockWebServerTest {
    private final TestWebServer server = new TestWebServer();

    @Before public void setUp() throws Exception {
        server.setUp();
    }

    @After public void tearDown() throws Exception {
        server.tearDown();
    }

    @NonNull protected TestWebServer server() {
        return server;
    }
}