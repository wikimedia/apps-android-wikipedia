package org.wikipedia.database.http;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.database.async.AsyncRow;

public class HttpRow extends AsyncRow<HttpStatus> {
    private static final HttpStatus DEFAULT_STATUS = HttpStatus.SYNCHRONIZED;

    public HttpRow(@NonNull String key) {
        super(key, DEFAULT_STATUS);
    }

    public HttpRow(@NonNull String key, @NonNull HttpStatus status, long timestamp,
                   long transactionId) {
        super(key, status, timestamp, transactionId);
    }

    @Override
    public boolean completable(@Nullable AsyncRow<HttpStatus> query) {
        boolean recordable = !(query == null && status() == HttpStatus.DELETED);
        return super.completable(query) && recordable;
    }

    @Override
    public void completeTransaction(long timestamp) {
        super.completeTransaction(timestamp);
        resetTransaction(DEFAULT_STATUS);
    }
}