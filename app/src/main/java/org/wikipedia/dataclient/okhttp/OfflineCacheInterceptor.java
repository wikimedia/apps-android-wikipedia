package org.wikipedia.dataclient.okhttp;

import androidx.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.RestService;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import okhttp3.CacheDelegate;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.cache.CacheRequest;
import okhttp3.internal.cache.CacheStrategy;
import okhttp3.internal.http.ExchangeCodec;
import okhttp3.internal.http.HttpHeaders;
import okhttp3.internal.http.RealResponseBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Sink;
import okio.Source;
import okio.Timeout;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static okhttp3.Protocol.HTTP_1_1;
import static okhttp3.internal.Util.discard;
import static org.wikipedia.util.SavedPagesConversionUtil.CONVERTED_FILES_DIRECTORY_NAME;
import static org.wikipedia.util.SavedPagesConversionUtil.PAGE_SUMMARY_DIRECTORY_NAME;

public class OfflineCacheInterceptor implements Interceptor {
    public static final String SAVE_HEADER = "X-Offline-Save";
    public static final String SAVE_HEADER_SAVE = "save";
    public static final String SAVE_HEADER_DELETE = "delete";
    static final String SAVE_HEADER_NONE = "none";
    private static final String PAGE_SUMMARY = "page/summary";
    private static final MediaType MEDIA_JSON = MediaType.parse("application/json");
    private static final MediaType TEXT_HTML = MediaType.parse("text/html");
    private static final int STATUS_CODE_SUCCESS = 200;

    @NonNull private final CacheDelegate cacheDelegate;

    OfflineCacheInterceptor(@NonNull CacheDelegate cache) {
        cacheDelegate = cache;
    }

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();

        // Prepare a candidate from the offline cache, but don't return it quite yet...
        Response cacheCandidate = null;
        // Only bother looking for a cached response if the request is cache-worthy
        if ((isCacheable(request) || SAVE_HEADER_DELETE.equals(request.header(SAVE_HEADER)))
                && cacheDelegate.isCached(request.url().toString())) {
            cacheCandidate = cacheDelegate.internalCache().get(request);
        }

        if (cacheCandidate == null && request.url().toString().contains(PAGE_SUMMARY)) {
            int indexOfTitleDelimiter = request.url().toString().lastIndexOf('/');
            File file = new File(WikipediaApp.getInstance().getFilesDir() + "/" + CONVERTED_FILES_DIRECTORY_NAME + PAGE_SUMMARY_DIRECTORY_NAME + "/" + request.url().toString().substring(indexOfTitleDelimiter + 1));
            if (file.exists()) {
                FileInputStream fileInputStream = new FileInputStream(file);
                String contentJSON = FileUtil.readFile(fileInputStream);
                cacheCandidate = new Response.Builder()
                        .code(STATUS_CODE_SUCCESS)
                        .request(request)
                        .protocol(HTTP_1_1)
                        .message("")
                        .body(ResponseBody.create(MEDIA_JSON, contentJSON))
                        .build();
            }
        }

        if (cacheCandidate == null && request.url().toString().contains(RestService.PAGE_HTML_ENDPOINT)) {
            int indexOfTitleDelimiter = request.url().toString().lastIndexOf('/');
            File file = new File(WikipediaApp.getInstance().getFilesDir() + "/" + CONVERTED_FILES_DIRECTORY_NAME + "/" + request.url().toString().substring(indexOfTitleDelimiter + 1));
            if (file.exists()) {
                FileInputStream fileInputStream = new FileInputStream(file);
                String contentJSON = FileUtil.readFile(fileInputStream);
                cacheCandidate = new Response.Builder()
                        .code(STATUS_CODE_SUCCESS)
                        .request(request)
                        .protocol(HTTP_1_1)
                        .message("")
                        .body(ResponseBody.create(TEXT_HTML, contentJSON))
                        .build();
            }
        }

        if (cacheCandidate != null) {
            // Tack on our special header onto it, so that receivers of the cache response
            // can know that it came from the offline cache.
            cacheCandidate = cacheCandidate.newBuilder()
                    .header(SAVE_HEADER, SAVE_HEADER_SAVE)
                    .build();
        }

        // If we're asked to delete the cached response, then delete it!
        if (SAVE_HEADER_DELETE.equals(request.header(SAVE_HEADER))) {
            // If we don't actually have a cache candidate, then something is very wrong.
            if (cacheCandidate == null) {
                throw new IOException("Requested to delete nonexistent cached response.");
            }
            try {
                cacheDelegate.internalCache().remove(request);
            } catch (IOException ignored) {
                // The cache cannot be written.
            }
            return cacheCandidate;
        }

        // If we're preferring offline content, and we have a cache candidate, then
        // just return it, since there's no point in trying a network request.
        if (Prefs.preferOfflineContent() && cacheCandidate != null && isCacheable(request)) {
            return cacheCandidate;
        }

        Response networkResponse = null;
        try {
            // Allow the call to proceed, and get the response, which might return fresher
            // content from the network
            networkResponse = chain.proceed(request);
            if (networkResponse.isSuccessful() && HttpHeaders.hasBody(networkResponse)
                    && networkResponse.networkResponse() != null
                    && CacheStrategy.isCacheable(networkResponse, request)
                    && isCacheableForOffline(request)) {

                // Cache (or re-cache) the response, overwriting any previous version.
                CacheRequest cacheRequest = cacheDelegate.internalCache().put(networkResponse);
                return cacheWritingResponse(cacheRequest, networkResponse);
            }
            return networkResponse;
        } catch (Throwable t) {
            if (networkResponse != null && networkResponse.isSuccessful()) {
                // The network call threw an exception, but actually returned a cached response.
                return networkResponse;
            }
            // The network call failed, so if we have a cache candidate, then return it!
            if (cacheCandidate != null) {
                return cacheCandidate;
            }
            throw t;
        }
    }

    private static boolean isCacheable(@NonNull Request request) {
        return "GET".equals(request.method()) && !request.cacheControl().noCache();
    }

    static boolean isCacheableForOffline(@NonNull Request request) {
        return SAVE_HEADER_SAVE.equals(request.header(SAVE_HEADER));
    }

    /**
     * Note: This method is lifted from okhttp3/CacheInterceptor.java.  Keep an eye out for
     * changes to that file, and import updates here if necessary.
     *
     * Returns a new source that writes bytes to {@code cacheRequest} as they are read by the source
     * consumer. This is careful to discard bytes left over when the stream is closed; otherwise we
     * may never exhaust the source stream and therefore not complete the cached response.
     */
    private Response cacheWritingResponse(final CacheRequest cacheRequest, Response response) throws IOException {
        // Some apps return a null body; for compatibility we treat that like a null cache request.
        if (cacheRequest == null) {
            return response;
        }
        Sink cacheBodyUnbuffered = cacheRequest.body();
        if (cacheBodyUnbuffered == null) {
            return response;
        }

        final BufferedSource source = response.body().source();
        final BufferedSink cacheBody = Okio.buffer(cacheBodyUnbuffered);

        Source cacheWritingSource = new Source() {
            boolean cacheRequestClosed;

            @Override
            public long read(@NonNull Buffer sink, long byteCount) throws IOException {
                long bytesRead;
                try {
                    bytesRead = source.read(sink, byteCount);
                } catch (IOException e) {
                    if (!cacheRequestClosed) {
                        cacheRequestClosed = true;
                        cacheRequest.abort(); // Failed to write a complete cache response.
                    }
                    throw e;
                }

                if (bytesRead == -1) {
                    if (!cacheRequestClosed) {
                        cacheRequestClosed = true;
                        cacheBody.close(); // The cache response is complete!
                    }
                    return -1;
                }

                sink.copyTo(cacheBody.buffer(), sink.size() - bytesRead, bytesRead);
                cacheBody.emitCompleteSegments();
                return bytesRead;
            }

            @Override public Timeout timeout() {
                return source.timeout();
            }

            @Override public void close() throws IOException {
                if (!cacheRequestClosed
                        && !discard(this, ExchangeCodec.DISCARD_STREAM_TIMEOUT_MILLIS, MILLISECONDS)) {
                    cacheRequestClosed = true;
                    cacheRequest.abort();
                }
                source.close();
            }
        };

        String contentType = response.header("Content-Type");
        long contentLength = response.body().contentLength();
        return response.newBuilder()
                .body(new RealResponseBody(contentType, contentLength, Okio.buffer(cacheWritingSource)))
                .build();
    }
}
