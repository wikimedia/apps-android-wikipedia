package org.wikipedia.test;

import androidx.annotation.NonNull;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory;

import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;

@RunWith(RobolectricTestRunner.class)
public abstract class MockWebServerTest {
    private OkHttpClient okHttpClient;
    private final TestWebServer server = new TestWebServer();

    @Before public void setUp() throws Throwable {
        OkHttpClient.Builder builder = OkHttpConnectionFactory.getClient().newBuilder();
        okHttpClient = builder.dispatcher(new Dispatcher(new ImmediateExecutorService())).build();
        server.setUp();
    }

    @After public void tearDown() throws Throwable {
        server.tearDown();
    }

    @NonNull protected TestWebServer server() {
        return server;
    }

    protected void enqueueFromFile(@NonNull String filename) throws Throwable {
        String json = TestFileUtil.readRawFile(filename);
        server.enqueue(json);
    }

    protected void enqueue404() {
        final int code = 404;
        server.enqueue(new MockResponse().setResponseCode(code).setBody("Not Found"));
    }

    protected void enqueueMalformed() {
        server.enqueue("(╯°□°）╯︵ ┻━┻");
    }

    protected void enqueueEmptyJson() {
        server.enqueue(new MockResponse().setBody("{}"));
    }

    @NonNull protected OkHttpClient okHttpClient() {
        return okHttpClient;
    }
}
