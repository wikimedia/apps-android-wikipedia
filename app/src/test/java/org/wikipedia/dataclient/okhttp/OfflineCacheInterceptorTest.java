package org.wikipedia.dataclient.okhttp;

import org.junit.Test;
import org.wikipedia.dataclient.Service;

import okhttp3.Request;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class OfflineCacheInterceptorTest {
    @Test public void testIsCacheableTrue() {
        Request req = new Request.Builder()
                .url(Service.WIKIPEDIA_URL)
                .addHeader(OfflineCacheInterceptor.SAVE_HEADER, OfflineCacheInterceptor.SAVE_HEADER_SAVE)
                .build();
        assertThat(OfflineCacheInterceptor.shouldSave(req), is(true));
    }

    @Test public void testIsCacheableFalse() {
        Request req = new Request.Builder().url(Service.WIKIPEDIA_URL).build();
        assertThat(OfflineCacheInterceptor.shouldSave(req), is(false));
    }
}
