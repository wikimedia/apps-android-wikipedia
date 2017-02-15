package org.wikipedia.database.http;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.database.async.AsyncRow;

public class HttpRow<T> extends AsyncRow<HttpStatus, T> {
    public HttpRow(@NonNull String key, @Nullable T dat) {
        super(key, HttpStatus.SYNCHRONIZED, dat);
    }

    public HttpRow(@NonNull HttpRow<T> httpRow, @Nullable T dat) {
        super(httpRow, dat);
    }

    public HttpRow(@NonNull String key, @NonNull HttpStatus status, long timestamp,
                   long transactionId) {
        super(key, status, timestamp, transactionId);
    }

    @Override public boolean completable(@Nullable AsyncRow<HttpStatus, T> query) {
        boolean recordable = !(query == null && status() == HttpStatus.DELETED);
        return super.completable(query) && recordable;
    }

    @Override public void completeTransaction(long timestamp) {
        super.completeTransaction(timestamp);
        resetTransaction(next(status()));
    }

    @NonNull private HttpStatus next(@NonNull HttpStatus current) {
        switch (current) {
            case SYNCHRONIZED:
            case OUTDATED:
            case MODIFIED:
            case ADDED:
                return HttpStatus.SYNCHRONIZED;
            case DELETED:
                return HttpStatus.DELETED;
            default:
                throw new RuntimeException("current=" + current);
        }
    }
}
