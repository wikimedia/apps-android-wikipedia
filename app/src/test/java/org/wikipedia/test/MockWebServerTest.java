package org.wikipedia.test;

import android.support.annotation.NonNull;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.wikipedia.json.GsonUtil;

import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@RunWith(TestRunner.class)
public abstract class MockWebServerTest {
    private final TestWebServer server = new TestWebServer();

    @Before public void setUp() throws Throwable {
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

    @NonNull public <T> T service(Class<T> clazz) {
        return service(clazz, server().getUrl());
    }

    @NonNull public <T> T service(Class<T> clazz, @NonNull String url) {
        OkHttpClient okHttp = new OkHttpClient.Builder()
                .dispatcher(new Dispatcher(new ImmediateExecutorService()))
                .build();
        return new Retrofit.Builder()
                .baseUrl(url)
                .callbackExecutor(new ImmediateExecutor())
                .client(okHttp)
                .addConverterFactory(GsonConverterFactory.create(GsonUtil.getDefaultGson()))
                .build()
                .create(clazz);
    }
}