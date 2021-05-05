package org.wikipedia.dataclient.okhttp;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.offline.OfflineObject;
import org.wikipedia.offline.OfflineObjectDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.UriUtil;
import org.wikipedia.util.log.L;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;
import okio.Timeout;

public class OfflineCacheInterceptor implements Interceptor {
    public static final String LANG_HEADER = "X-Offline-Lang";
    public static final String TITLE_HEADER = "X-Offline-Title";
    public static final String SAVE_HEADER = "X-Offline-Save";
    public static final String SAVE_HEADER_SAVE = "save";

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        Response response;
        IOException networkException;

        String lang = request.header(LANG_HEADER);
        String title = UriUtil.decodeURL(StringUtils.defaultString(request.header(TITLE_HEADER)));

        // attempt to read from the network.
        try {
            response = chain.proceed(request);
            // is this response worthy of caching offline?
            if (response.isSuccessful() && response.networkResponse() != null && shouldSave(request)
                    && !TextUtils.isEmpty(lang) && !TextUtils.isEmpty(title)) {

                // Cache (or re-cache) the response, overwriting any previous version.
                return getCacheWritingResponse(request, response, lang, title);

            }
            return response;
        } catch (IOException t) {
            networkException = t;
        }

        // If we're here, then the network call failed.
        // Time to see if we can load this content from offline storage.

        String url = request.url().toString();

        // If we don't have the correct headers to retrieve this item, then bail.
        if (TextUtils.isEmpty(lang)) {
            // ...unless we're looking for an image from Commons, in which case we'll try to match it by URL only.
            if (url.contains("/commons/")) {
                lang = "";
            } else {
                throw networkException;
            }
        }

        OfflineObject obj = OfflineObjectDbHelper.instance().findObject(url, lang);
        if (obj == null) {
            L.w("Offline object not present in database.");
            throw networkException;
        }

        File metadataFile = new File(obj.getPath() + ".0");
        File contentsFile = new File(obj.getPath() + ".1");

        if (!metadataFile.exists() || !contentsFile.exists()) {
            throw new IOException("Offline object not present in filesystem.");
        }

        Response.Builder builder = new Response.Builder().request(request)
                .protocol(Protocol.HTTP_2);

        String contentType = "*/*";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(metadataFile)))) {
            reader.readLine(); // url
            reader.readLine(); // method
            reader.readLine(); // protocol
            builder.code(Integer.parseInt(reader.readLine()));
            String message = reader.readLine();
            builder.message(TextUtils.isEmpty(message) ? "OK" : message);
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                int pos = line.indexOf(":");
                if (pos < 0) {
                    break;
                }
                String name = line.substring(0, pos).trim();
                String value = line.substring(pos + 1).trim();
                builder.header(name, value);
                if (name.toLowerCase().equals("content-type")) {
                    contentType = value;
                }
            }
        } catch (IOException e) {
            L.e(e);
        }

        // since we're returning this response manually, let's tell the network library not to cache it.
        builder.header("Cache-Control", "no-cache");
        // and tack on the Save header, so that the recipient knows that this response came from offline cache.
        builder.header(SAVE_HEADER, SAVE_HEADER_SAVE);

        builder.body(new CachedResponseBody(contentsFile, contentType));
        response = builder.build();
        return response;
    }

    public static boolean shouldSave(@NonNull Request request) {
        return "GET".equals(request.method()) && SAVE_HEADER_SAVE.equals(request.header(SAVE_HEADER));
    }

    private static String getObjectFileName(@NonNull String url, @NonNull String lang, @NonNull String mimeType) {
        // If the object is an image, then make the hash independent of language.
        // Otherwise, encode the language into the hash.
        return mimeType.startsWith("image") ? StringUtil.md5string(url) : StringUtil.md5string(lang + ":" + url);
    }

    private Response getCacheWritingResponse(@NonNull Request request, @NonNull Response response,
                                             @NonNull String lang, @NonNull String title) {
        String contentType = response.header("Content-Type", "*/*");
        long contentLength = Long.parseLong(response.header("Content-Length", "-1"));

        String cachePath = WikipediaApp.getInstance().getFilesDir().getAbsolutePath()
                + File.separator + OfflineObjectDbHelper.OFFLINE_PATH;
        new File(cachePath).mkdirs();

        String filePath = cachePath + File.separator + getObjectFileName(request.url().toString(), lang, contentType);

        File metadataFile = new File(filePath + ".0");
        File contentsFile = new File(filePath + ".1");

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(metadataFile))) {
            writer.write(request.url().toString() + "\n");
            writer.write(request.method() + "\n");
            writer.write(response.protocol() + "\n");
            writer.write(response.code() + "\n");
            writer.write(response.message() + "\n");
            for (String header : response.headers().names()) {
                writer.write(header + ": " + response.header(header) + "\n");
            }
            writer.flush();
        } catch (IOException e) {
            L.e(e);
            return response;
        }

        BufferedSink sink;
        try {
            sink = Okio.buffer(Okio.sink(contentsFile));
        } catch (IOException e) {
            L.e(e);
            return response;
        }

        OfflineObject obj = new OfflineObject(request.url().toString(), lang, filePath, 0);
        Source cacheWritingSource = new CacheWritingSource(response.body().source(), sink, obj, title);

        return response.newBuilder()
                .body(new CacheWritingResponseBody(cacheWritingSource, contentType, contentLength))
                .build();
    }

    private class CacheWritingSource implements Source {
        private boolean cacheRequestClosed;
        private boolean failed;
        private BufferedSource source;
        private BufferedSink cacheSink;
        private OfflineObject obj;
        private String title;

        CacheWritingSource(BufferedSource source, BufferedSink sink, OfflineObject obj, String title) {
            this.source = source;
            this.cacheSink = sink;
            this.obj = obj;
            this.title = title;
        }

        @Override
        public long read(@NonNull Buffer sink, long byteCount) throws IOException {
            long bytesRead;
            try {
                bytesRead = source.read(sink, byteCount);
            } catch (IOException e) {
                failed = true;
                if (!cacheRequestClosed) {
                    cacheRequestClosed = true;
                    // Failed to write a complete cache response.
                }
                throw e;
            }

            if (bytesRead == -1) {
                if (!cacheRequestClosed) {
                    cacheRequestClosed = true;
                    // The cache response is complete!
                    cacheSink.close();

                    if (!failed) {
                        // update the record in the database!
                        OfflineObjectDbHelper.instance().addObject(obj.getUrl(), obj.getLang(), obj.getPath(), title);
                    }
                }
                return -1;
            }

            sink.copyTo(cacheSink.getBuffer(), sink.size() - bytesRead, bytesRead);
            cacheSink.emitCompleteSegments();
            return bytesRead;
        }

        @Override public Timeout timeout() {
            failed = true;
            return source.timeout();
        }

        @Override public void close() throws IOException {
            if (!cacheRequestClosed) {
                // discard(this, ExchangeCodec.DISCARD_STREAM_TIMEOUT_MILLIS, MILLISECONDS)
                cacheRequestClosed = true;
            }
            source.close();
            if (failed) {
                OfflineObjectDbHelper.deleteFilesForObject(obj);
            }
        }
    }

    private class CacheWritingResponseBody extends ResponseBody {
        private Source source;
        private String contentType;
        private long contentLength;

        CacheWritingResponseBody(Source source, String contentType, long contentLength) {
            this.source = source;
            this.contentType = contentType;
            this.contentLength = contentLength;
        }

        @Override public MediaType contentType() {
            return contentType != null ? MediaType.parse(contentType) : null;
        }

        @Override public long contentLength() {
            return contentLength;
        }

        @Override public BufferedSource source() {
            return Okio.buffer(source);
        }
    }

    private class CachedResponseBody extends ResponseBody {
        private File file;
        private String contentType;

        CachedResponseBody(File file, String contentType) {
            this.file = file;
            this.contentType = contentType;
        }

        @Override
        public MediaType contentType() {
            return MediaType.parse(contentType);
        }

        @Override
        public long contentLength() {
            return -1;
        }

        @Override
        public BufferedSource source() {
            try {
                return Okio.buffer(Okio.source(file));
            } catch (Exception e) {
                return null;
            }
        }
    }

    // TODO: Remove after 2 releases
    public static void createCacheItemFor(ReadingListPage page, String url, String contents, String mimeType, String dateModified) {
        String cachePath = WikipediaApp.getInstance().getFilesDir().getAbsolutePath()
                + File.separator + OfflineObjectDbHelper.OFFLINE_PATH;
        new File(cachePath).mkdirs();

        String filePath = cachePath + File.separator + getObjectFileName(url, page.getLang(), mimeType);

        File metadataFile = new File(filePath + ".0");
        File contentsFile = new File(filePath + ".1");

        if (metadataFile.exists()) {
            return;
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(metadataFile))) {
            writer.write(url + "\n");
            writer.write("GET\n");
            writer.write("H2\n");
            writer.write("200\n");
            writer.write("OK\n");
            writer.write("content-type: " + mimeType + "\n");
            writer.write("content-length: " + contents.length() + "\n");
            writer.write("date: " + dateModified + "\n");
            writer.flush();
        } catch (IOException e) {
            L.e(e);
            return;
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(contentsFile))) {
            writer.write(contents);
            writer.flush();
        } catch (IOException e) {
            L.e(e);
            return;
        }

        OfflineObjectDbHelper.instance().addObject(url, page.getLang(), filePath, page.getApiTitle());
    }

}
