package org.wikipedia.database.http;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.database.async.DefaultAsyncRow;
import org.wikipedia.database.async.AsyncRow;

public class HttpRow extends DefaultAsyncRow<HttpStatus> {
    @NonNull private HttpStatus status;

    public HttpRow() {
        status = HttpStatus.SYNCHRONIZED;
    }

    public HttpRow(@NonNull HttpStatus status, long timestamp, long transactionId) {
        super(timestamp, transactionId);
        this.status = status;
    }

    @NonNull @Override public HttpStatus status() {
        return status;
    }

    @Override
    public int statusCode() {
        return status().code();
    }

    @Override
    public void resetTransaction(@NonNull HttpStatus status) {
        this.status = status;
        super.resetTransaction(status);
    }

    @Override
    public boolean completable(@Nullable AsyncRow<HttpStatus> query) {
        boolean recordable = !(query == null && status() == HttpStatus.DELETED);
        return super.completable(query) && recordable;
    }

    @Override
    public void completeTransaction(long timestamp) {
        super.completeTransaction(timestamp);
        resetTransaction(HttpStatus.SYNCHRONIZED);
    }
}
