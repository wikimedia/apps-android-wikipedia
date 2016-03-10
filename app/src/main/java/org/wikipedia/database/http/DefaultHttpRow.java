package org.wikipedia.database.http;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class DefaultHttpRow implements HttpRow {
    public static final int NO_TRANSACTION_ID = 0;

    @NonNull private HttpStatus status;
    private long transactionId;
    private long timestamp;

    public DefaultHttpRow() {
        this(HttpStatus.SYNCHRONIZED, NO_TRANSACTION_ID, 0);
    }

    public DefaultHttpRow(@NonNull HttpStatus status, long transactionId, long timestamp) {
        this.status = status;
        this.transactionId = transactionId;
        this.timestamp = timestamp;
    }

    @Override
    @NonNull
    public HttpStatus status() {
        return status;
    }

    @Override
    public long transactionId() {
        return transactionId;
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public void resetTransaction(@NonNull HttpStatus status) {
        this.status = status;
        this.transactionId = NO_TRANSACTION_ID;
    }

    @Override
    public void startTransaction() {
        transactionId = newTransactionId();
    }

    @Override
    public boolean completeable(@Nullable HttpRow old) {
        boolean newer = old == null || transactionId() == NO_TRANSACTION_ID;
        boolean response = old != null && transactionId() == old.transactionId();
        boolean recordable = !(old == null && status() == HttpStatus.DELETED);
        return (newer || response) && recordable;
    }

    @Override
    public void completeTransaction(long timestamp) {
        this.timestamp = timestamp;
        resetTransaction(HttpStatus.SYNCHRONIZED);
    }

    private long newTransactionId() {
        return System.nanoTime();
    }
}