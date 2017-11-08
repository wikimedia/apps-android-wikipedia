package org.wikipedia.dataclient.page;

import android.support.annotation.NonNull;

import org.junit.Test;
import org.wikipedia.Constants;
import org.wikipedia.dataclient.okhttp.cache.SaveHeader;
import org.wikipedia.test.MockWebServerTest;

import okhttp3.CacheControl;
import retrofit2.Call;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public abstract class BasePageClientTest extends MockWebServerTest {
    @Test public void testLeadCacheControl() throws Throwable {
        Call<?> call = subject().lead(CacheControl.FORCE_NETWORK, PageClient.CacheOption.CACHE, "", 0);
        assertThat(call.request().header("Cache-Control"), containsString("no-cache"));
    }

    @Test public void testLeadNoCacheControl() throws Throwable {
        Call<?> call = subject().lead(null, PageClient.CacheOption.CACHE, "", 0);
        assertThat(call.request().header("Cache-Control"), nullValue());
    }

    @Test public void testLeadCacheOptionCache() throws Throwable {
        Call<?> call = subject().lead(null, PageClient.CacheOption.CACHE, "", 0);
        assertThat(call.request().header(SaveHeader.FIELD), nullValue());
    }

    @Test public void testLeadCacheOptionSave() throws Throwable {
        Call<?> call = subject().lead(null, PageClient.CacheOption.SAVE, "", 0);
        assertThat(call.request().header(SaveHeader.FIELD), is(SaveHeader.VAL_ENABLED));
    }

    @Test public void testLeadTitle() throws Throwable {
        Call<?> call = subject().lead(null, PageClient.CacheOption.CACHE, "title", 0);
        assertThat(call.request().url().toString(), containsString("title"));
    }

    @Test public void testSectionsCacheControl() throws Throwable {
        Call<?> call = subject().sections(CacheControl.FORCE_NETWORK, PageClient.CacheOption.CACHE, "");
        assertThat(call.request().header("Cache-Control"), containsString("no-cache"));
    }

    @Test public void testSectionsNoCacheControl() throws Throwable {
        Call<?> call = subject().sections(null, PageClient.CacheOption.CACHE, "");
        assertThat(call.request().header("Cache-Control"), nullValue());
    }

    @Test public void testSectionsCacheOptionCache() throws Throwable {
        Call<?> call = subject().sections(null, PageClient.CacheOption.CACHE, "");
        assertThat(call.request().header(SaveHeader.FIELD), nullValue());
    }

    @Test public void testSectionsCacheOptionSave() throws Throwable {
        Call<?> call = subject().sections(null, PageClient.CacheOption.SAVE, "");
        assertThat(call.request().header(SaveHeader.FIELD), is(SaveHeader.VAL_ENABLED));
    }

    @Test public void testSectionsTitle() throws Throwable {
        Call<?> call = subject().sections(null, PageClient.CacheOption.CACHE, "title");
        assertThat(call.request().url().toString(), containsString("title"));
    }

    @NonNull protected abstract PageClient subject();

    protected String preferredThumbSizeString() {
        return Integer.toString(Constants.PREFERRED_THUMB_SIZE) + "px";
    }
}
