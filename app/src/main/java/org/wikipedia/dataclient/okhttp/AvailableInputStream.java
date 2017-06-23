package org.wikipedia.dataclient.okhttp;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;

/**
 * This is a subclass of InputStream that implements the available() method reliably enough
 * to satisfy WebResourceResponses or other consumers like BufferedInputStream that depend
 * on available() to return a meaningful value.
 *
 * The problem is that the InputStream provided by OkHttp's body().byteStream() returns zero
 * when calling available() prior to making any read() calls, which means that it will break
 * any consumers that wrap a BufferedInputStream onto this stream, or any other wrapper that
 * relies on a consistent implementation of available().
 *
 * This is initialized with the original InputStream plus its total size, which must be known
 * at the time of instantiation.  You may then call the read() and skip() methods in the usual
 * way, and then be able to call available() and get the number of bytes left to read.
 */
public class AvailableInputStream extends InputStream {
    private InputStream stream;
    private long available;

    public AvailableInputStream(InputStream stream, long available) {
        this.stream = stream;
        this.available = available;
    }

    @Override public int read() throws IOException {
        decreaseAvailable(1);
        return stream.read();
    }

    @Override public int read(@NonNull byte[] b) throws IOException {
        int ret = stream.read(b);
        if (ret > 0) {
            decreaseAvailable(ret);
        }
        return ret;
    }

    @Override public int read(@NonNull byte[] b, int off, int len) throws IOException {
        int ret = stream.read(b, off, len);
        if (ret > 0) {
            decreaseAvailable(ret);
        }
        return ret;
    }

    @Override public long skip(long n) throws IOException {
        long ret = stream.skip(n);
        if (ret > 0) {
            decreaseAvailable(ret);
        }
        return ret;
    }

    @Override public int available() throws IOException {
        int ret = stream.available();
        if (ret == 0 && available > 0) {
            return (int) available;
        }
        return ret;
    }

    private void decreaseAvailable(long n) {
        available -= n;
        if (available < 0) {
            available = 0;
        }
    }
}
