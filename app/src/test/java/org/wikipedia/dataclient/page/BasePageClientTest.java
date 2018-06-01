package org.wikipedia.dataclient.page;

import android.support.annotation.NonNull;

import org.junit.Test;
import org.wikipedia.Constants;
import org.wikipedia.dataclient.okhttp.OfflineCacheInterceptor;
import org.wikipedia.test.MockWebServerTest;

import okhttp3.CacheControl;
import retrofit2.Call;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public abstract class BasePageClientTest extends MockWebServerTest {
    @Test public void testLeadCacheControl() throws Throwable {
        Call<?> call = subject().lead(CacheControl.FORCE_NETWORK, null, null, "", 0);
        assertThat(call.request().header("Cache-Control"), containsString("no-cache"));
    }

    @Test public void testLeadNoCacheControl() throws Throwable {
        Call<?> call = subject().lead(null, null, null, "", 0);
        assertThat(call.request().header("Cache-Control"), nullValue());
    }

    @Test public void testLeadHttpRefererUrl() throws Throwable {
        String refererUrl = "https://en.wikipedia.org/wiki/United_States";
        Call<?> call = subject().lead(null, null, refererUrl, "", 0);
        assertThat(call.request().header("Referer"), containsString(refererUrl));
    }

    @Test public void testLeadCacheOptionCache() throws Throwable {
        Call<?> call = subject().lead(null, null, null, "", 0);
        assertThat(call.request().header(OfflineCacheInterceptor.SAVE_HEADER), nullValue());
    }

    @Test public void testLeadCacheOptionSave() throws Throwable {
        Call<?> call = subject().lead(null, OfflineCacheInterceptor.SAVE_HEADER_SAVE, null, "", 0);
        assertThat(call.request().header(OfflineCacheInterceptor.SAVE_HEADER), is(OfflineCacheInterceptor.SAVE_HEADER_SAVE));
    }

    @Test public void testLeadTitle() throws Throwable {
        Call<?> call = subject().lead(null, null, null, "title", 0);
        assertThat(call.request().url().toString(), containsString("title"));
    }

    @Test public void testSectionsCacheControl() throws Throwable {
        Call<?> call = subject().sections(CacheControl.FORCE_NETWORK, null, "");
        assertThat(call.request().header("Cache-Control"), containsString("no-cache"));
    }

    @Test public void testSectionsNoCacheControl() throws Throwable {
        Call<?> call = subject().sections(null, null, "");
        assertThat(call.request().header("Cache-Control"), nullValue());
    }

    @Test public void testSectionsCacheOptionCache() throws Throwable {
        Call<?> call = subject().sections(null, null, "");
        assertThat(call.request().header(OfflineCacheInterceptor.SAVE_HEADER), nullValue());
    }

    @Test public void testSectionsCacheOptionSave() throws Throwable {
        Call<?> call = subject().sections(null, OfflineCacheInterceptor.SAVE_HEADER_SAVE, "");
        assertThat(call.request().header(OfflineCacheInterceptor.SAVE_HEADER), is(OfflineCacheInterceptor.SAVE_HEADER_SAVE));
    }

    @Test public void testSectionsTitle() throws Throwable {
        Call<?> call = subject().sections(null, null, "title");
        assertThat(call.request().url().toString(), containsString("title"));
    }

    @NonNull protected abstract PageClient subject();

    protected String preferredThumbSizeString() {
        return Integer.toString(Constants.PREFERRED_THUMB_SIZE) + "px";
    }
}
