package okhttp3.internal.cache;

import android.support.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory;
import org.wikipedia.dataclient.okhttp.cache.SaveHeader;
import org.wikipedia.test.MockWebServerTest;

import okhttp3.CacheControl;
import okhttp3.CacheDelegate;
import okhttp3.Request;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CacheDelegateInterceptorTest extends MockWebServerTest {
    private static final String URL = "url";

    @NonNull private final CacheDelegate netCache = new CacheDelegate(OkHttpConnectionFactory.NET_CACHE);
    @NonNull private final CacheDelegate saveCache = new CacheDelegate(OkHttpConnectionFactory.SAVE_CACHE);

    @Before public void setUp() throws Throwable {
        super.setUp();

        Request req = newRequest();
        netCache.remove(req);
        saveCache.remove(req);
    }

    // Sanity check that both caches are truly empty
    @Test(expected = HttpStatusException.class) public void testAssumptions() throws Throwable {
        Request req = newOnlyIfCachedRequest();

        assertCached(netCache, req, false);
        assertCached(saveCache, req, false);

        executeRequest(req);
    }

    @Test public void testInterceptWriteNetCacheNoHeader() throws Throwable {
        Request req = newRequest();
        requestResponse("0", req);
        assertCached(netCache, req, true);
    }

    @Test public void testInterceptWriteSaveCacheNoHeader() throws Throwable {
        Request req = newRequest();
        requestResponse("0", req);
        assertCached(saveCache, req, false);
    }

    @Test public void testInterceptWriteNetCacheSaveHeaderEnabled() throws Throwable {
        Request req = newSaveEnabledRequest();
        requestResponse("0", req);
        assertCached(netCache, req, true);
    }

    @Test public void testInterceptWriteSaveCacheSaveHeaderEnabled() throws Throwable {
        Request req = newSaveEnabledRequest();
        requestResponse("0", req);
        assertCached(saveCache, req, true);
    }

    @Test public void testInterceptWriteNetCacheSaveHeaderDisabled() throws Throwable {
        Request req = newSaveDisabledRequest();
        requestResponse("0", req);
        assertCached(netCache, req, true);
    }

    @Test public void testInterceptWriteSaveCacheSaveHeaderDisabled() throws Throwable {
        Request req = newSaveDisabledRequest();
        requestResponse("0", req);
        assertCached(saveCache, req, false);
    }

    @Test public void testInterceptReadNetCacheNoHeader() throws Throwable {
        Request req = newRequest();
        requestResponse("0", req);

        saveCache.remove(req);
        requestOnlyIfCachedResponse("0", newOnlyIfCachedRequest());
    }

    @Test public void testInterceptReadSaveCacheNoHeader() throws Throwable {
        Request req = newRequest();
        requestResponse("0", newSaveEnabledRequest());

        netCache.remove(req);
        requestOnlyIfCachedResponse("0", newOnlyIfCachedRequest());
    }

    @Test public void testInterceptReadNetCacheSaveHeaderEnabled() throws Throwable {
        Request req = newSaveEnabledRequest();
        requestResponse("0", req);

        saveCache.remove(req);
        requestOnlyIfCachedResponse("0", newOnlyIfCachedRequest());
    }

    @Test public void testInterceptReadSaveCacheSaveHeaderEnabled() throws Throwable {
        Request req = newSaveEnabledRequest();
        requestResponse("0", req);

        netCache.remove(req);
        requestOnlyIfCachedResponse("0", newOnlyIfCachedRequest());
    }

    @Test public void testInterceptReadNetCacheSaveHeaderDisabled() throws Throwable {
        Request req = newSaveDisabledRequest();
        requestResponse("0", req);

        saveCache.remove(req);
        requestOnlyIfCachedResponse("0", newOnlyIfCachedRequest());
    }

    @Test public void testInterceptReadSaveCacheSaveHeaderDisabled() throws Throwable {
        Request req = newSaveDisabledRequest();
        requestResponse("0", newSaveEnabledRequest());

        netCache.remove(req);
        requestOnlyIfCachedResponse("0", newOnlyIfCachedRequest());
    }

    @Test public void testInterceptUpdateNetCacheNoHeader() throws Throwable {
        Request req = newRequest();
        requestResponse("0", req);
        requestResponse("1", req);

        saveCache.remove(req);
        requestOnlyIfCachedResponse("1", newOnlyIfCachedRequest());
    }

    @Test public void testInterceptUpdateSaveCacheNoHeader() throws Throwable {
        Request req = newRequest();
        requestResponse("0", newSaveEnabledRequest());
        requestResponse("1", req);

        netCache.remove(req);
        requestOnlyIfCachedResponse("1", newOnlyIfCachedRequest());
    }

    @Test public void testInterceptUpdateNetCacheSaveHeaderEnabled() throws Throwable {
        Request req = newRequest();
        requestResponse("0", req);
        requestResponse("1", newSaveEnabledRequest());

        saveCache.remove(req);
        requestOnlyIfCachedResponse("1", newOnlyIfCachedRequest());
    }

    @Test public void testInterceptUpdateSaveCacheSaveHeaderEnabled() throws Throwable {
        Request req = newRequest();
        requestResponse("0", req);
        requestResponse("1", newSaveEnabledRequest());

        netCache.remove(req);
        requestOnlyIfCachedResponse("1", newOnlyIfCachedRequest());
    }

    @Test public void testInterceptUpdateNetCacheSaveHeaderEnabledThenNoHeader() throws Throwable {
        Request req = newRequest();
        requestResponse("0", req);
        requestResponse("1", newSaveEnabledRequest());

        saveCache.remove(req);

        requestResponse("2", req);

        saveCache.remove(req);
        requestOnlyIfCachedResponse("2", newOnlyIfCachedRequest());
    }

    @Test public void testInterceptUpdateSaveCacheSaveHeaderEnabledThenNoHeader() throws Throwable {
        Request req = newRequest();
        requestResponse("0", req);
        requestResponse("1", newSaveEnabledRequest());

        netCache.remove(req);

        requestResponse("2", req);

        netCache.remove(req);
        requestOnlyIfCachedResponse("2", newOnlyIfCachedRequest());
    }

    @Test public void testInterceptErrorNetCache() throws Throwable {
        Request req = newRequest();
        requestResponse("0", req);

        saveCache.remove(req);

        requestResponseError("0", req);
    }

    @Test public void testInterceptErrorSaveCache() throws Throwable {
        Request req = newSaveEnabledRequest();
        requestResponse("0", req);

        netCache.remove(req);

        requestResponseError("0", req);
    }

    private void requestResponseError(@NonNull String body, @NonNull Request req) throws Throwable {
        enqueue404();
        String rsp = executeRequest(req);
        server().takeRequest();
        assertThat(rsp, is(body));
    }

    private void requestResponse(@NonNull String body, @NonNull Request req) throws Throwable {
        server().enqueue(body);
        String rsp = executeRequest(req);
        server().takeRequest();
        assertThat(rsp, is(body));
    }

    private void requestOnlyIfCachedResponse(@NonNull String body, @NonNull Request req) throws Throwable {
        String rsp = executeRequest(req);
        assertThat(rsp, is(body));
    }

    private String executeRequest(@NonNull Request req) throws Throwable {
        // Note: raw non-Retrofit usage of OkHttp Requests requires that the Response body is read
        // for the cache to be written.
        return okHttpClient().newCall(req).execute().body().string();
    }

    @NonNull private Request newOnlyIfCachedRequest() {
        return newRequest().newBuilder().cacheControl(CacheControl.FORCE_CACHE).build();
    }

    @NonNull private Request newSaveEnabledRequest() {
        return newRequest().newBuilder().header(SaveHeader.FIELD, SaveHeader.VAL_ENABLED).build();
    }

    @NonNull private Request newSaveDisabledRequest() {
        return newRequest().newBuilder().header(SaveHeader.FIELD, SaveHeader.VAL_DISABLED).build();
    }

    @NonNull private Request newRequest() {
        return new Request.Builder().url(server().getUrl(URL)).build();
    }

    private void assertCached(@NonNull CacheDelegate cacheDelegate, @NonNull Request req,
                              boolean cached) {
        assertThat(cacheDelegate.isCached(req.url().toString()), is(cached));
    }
}
