package org.wikipedia.dataclient.okhttp;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.offline.OfflineObject;
import org.wikipedia.offline.OfflineObjectDbHelper;
import org.wikipedia.util.StringUtil;
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
        IOException networkException = null;

        String lang = request.header(LANG_HEADER);
        String title = request.header(TITLE_HEADER);

        if (WikipediaApp.getInstance().isOnline()) {
            // attempt to read from the network.
            try {
                response = chain.proceed(request);
                // is this response worthy of caching offline?
                if (response.isSuccessful() && response.networkResponse() != null && isCacheable(request)
                        && !TextUtils.isEmpty(lang) && !TextUtils.isEmpty(title)) {

                    // Cache (or re-cache) the response, overwriting any previous version.
                    return getCacheWritingResponse(request, response, lang, title);

                }
                return response;
            } catch (IOException t) {
                networkException = t;
            }
        }

        // If we're here, then the network call failed.
        // Time to see if we can load this content from offline storage.

        if (!isCacheable(request) || TextUtils.isEmpty(lang) || TextUtils.isEmpty(title)) {
            throw networkException != null ? networkException : new IOException("Invalid headers when requesting offline object.");
        }

        OfflineObject obj = OfflineObjectDbHelper.instance().findObject(request.url().toString(), lang);
        if (obj == null) {
            throw networkException != null ? networkException : new IOException("Offline object not present in database.");
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

        builder.body(new CachedResponseBody(contentsFile, contentType));
        response = builder.build();
        return response;
    }

    private static boolean isCacheable(@NonNull Request request) {
        return "GET".equals(request.method()) && SAVE_HEADER_SAVE.equals(request.header(SAVE_HEADER));
    }

    private String getObjectFileName(@NonNull String url, @NonNull String lang, @NonNull String mimeType) {
        // If the object is an image, then make the hash independent of language.
        // Otherwise, encode the language into the hash.
        return mimeType.startsWith("image") ? StringUtil.md5string(url) : StringUtil.md5string(lang + ":" + url);
    }

    private Response getCacheWritingResponse(@NonNull Request request, @NonNull Response response,
                                             @NonNull String lang, @NonNull String title) {
        String contentType = response.header("Content-Type", "*/*");
        long contentLength = Long.parseLong(response.header("Content-Length", "0"));

        String cachePath = WikipediaApp.getInstance().getFilesDir().getAbsolutePath()
                + File.separator + OfflineObjectDbHelper.OFFLINE_PATH;
        new File(cachePath).mkdirs();

        final String filePath = cachePath + File.separator + getObjectFileName(request.url().toString(), lang, contentType);

        final File metadataFile = new File(filePath + ".0");
        final File contentsFile = new File(filePath + ".1");

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
        final BufferedSource source = response.body().source();
        final BufferedSink cacheFileSink = sink;

        Source cacheWritingSource = new Source() {
            boolean cacheRequestClosed;
            boolean failed;

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
                        cacheFileSink.close();

                        // update the record in the database!
                        OfflineObjectDbHelper.instance().addObject(request.url().toString(), lang, filePath, title);
                    }
                    return -1;
                }

                sink.copyTo(cacheFileSink.buffer(), sink.size() - bytesRead, bytesRead);
                cacheFileSink.emitCompleteSegments();
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
                    try {
                        metadataFile.delete();
                        contentsFile.delete();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        };

        return response.newBuilder()
                .body(new ResponseBody() {
                    @Override
                    public MediaType contentType() {
                        return contentType != null ? MediaType.parse(contentType) : null;
                    }

                    @Override
                    public long contentLength() {
                        return contentLength;
                    }

                    @Override
                    public BufferedSource source() {
                        return Okio.buffer(cacheWritingSource);
                    }
                })
                .build();
    }

    private class CachedResponseBody extends ResponseBody {
        private File file;
        private String contentType;

        public CachedResponseBody(File file, String contentType) {
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
}
