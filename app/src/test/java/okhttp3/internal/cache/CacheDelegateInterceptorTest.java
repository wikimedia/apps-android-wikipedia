package okhttp3.internal.cache;

import android.support.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory;
import org.wikipedia.dataclient.okhttp.cache.SaveHeader;
import org.wikipedia.test.ImmediateExecutorService;
import org.wikipedia.test.MockWebServerTest;

import java.nio.charset.StandardCharsets;

import okhttp3.CacheControl;
import okhttp3.CacheDelegate;
import okhttp3.Dispatcher;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okio.Buffer;
import okio.GzipSink;
import okio.Sink;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.wikipedia.dataclient.okhttp.cache.DiskLruCacheUtil.okHttpResponseBodySize;
import static org.wikipedia.dataclient.okhttp.cache.DiskLruCacheUtil.okHttpResponseMetadataSize;

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

    // Both the network and saved cache are expected to be empty after each test's setUp().
    @Test(expected = HttpStatusException.class) public void testAssumptionCacheIsEmptyAfterSetUp() throws Throwable {
        Request req = newOnlyIfCachedRequest();

        assertCached(netCache, req, false);
        assertCached(saveCache, req, false);

        executeRequest(req);
    }

    // The size on disk of an empty body is expected to be zero.
    @Test public void testAssumptionCacheSizeEmptyBody() throws Throwable {
        Request req = newRequest();
        requestResponse("", req);

        DiskLruCache.Snapshot snapshot = netCache.entry(req);

        assertThat(okHttpResponseBodySize(snapshot), is(0L));
    }

    // The size on disk of a nonempty body is expected to be nonzero.
    @Test public void testAssumptionCacheSizeNonemptyBody() throws Throwable {
        Request req = newRequest();
        requestResponse("A", req);

        DiskLruCache.Snapshot snapshot = netCache.entry(req);
        assertThat(okHttpResponseBodySize(snapshot), is(1L));
    }

    // The size on disk of OkHttp metadata is expected to be nonzero.
    @Test public void testAssumptionCacheSizeMetadataIsNonzero() throws Throwable {
        Request req = newRequest();
        requestResponse("A", req);

        DiskLruCache.Snapshot snapshot = netCache.entry(req);

        // The size on disk of OkHttp metadata overhead is expected to be nonzero and necessary to
        // consider when calculating disk usage for a page and all of it's resources so that more
        // just the Content-Length header need be considered for each resource response.
        assertThat(okHttpResponseMetadataSize(snapshot), notNullValue());
    }

    // Although OkHttp decompresses gzipped service responses seamlessly, the cache is expected to
    // persist them in compressed form and report the compressed size, not the decompressed size.
    @Test public void testAssumptionCacheSizeCompressedSizeIsReported() throws Throwable {
        String interval = "0123456789"; // One cycle.
        String body = StringUtils.repeat(interval, 100_000); // The body is many intervals.

        Buffer buffer = new Buffer();
        Sink sink = new GzipSink(buffer);
        Buffer uncompressedBuffer = new Buffer().writeString(body, StandardCharsets.UTF_8);
        long uncompressedSize = uncompressedBuffer.size();
        sink.write(uncompressedBuffer, uncompressedBuffer.size());
        sink.close();

        // The compressed size is expected to be worse than one interval but at least 100x better
        // than all intervals.
        long compressedSize = buffer.size();
        assertThat(compressedSize,
                allOf(greaterThan((long) interval.length()), lessThan(uncompressedSize / 100L)));

        // Enqueue a compressed response.
        MockResponse serviceResponse = new MockResponse()
                .addHeader("Content-Encoding", "gzip")
                .setBody(buffer);
        server().enqueue(serviceResponse);

        Request req = newRequest();
        String rsp = executeRequest(req);
        server().takeRequest();
        assertThat(rsp, is(body));

        DiskLruCache.Snapshot snapshot = netCache.entry(req);

        // The size on disk is expected to be the compressed size.
        assertThat(okHttpResponseBodySize(snapshot), is(compressedSize));
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
        return OkHttpConnectionFactory.getClient()
                .newBuilder()
                .dispatcher(new Dispatcher(new ImmediateExecutorService()))
                .build()
                .newCall(req).execute().body().string();
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
