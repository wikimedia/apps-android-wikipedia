package org.wikipedia.database.async;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public abstract class DefaultAsyncRow<T> implements AsyncRow<T> {
    public static final int NO_TRANSACTION_ID = 0;
    public static final int NO_TIMESTAMP = 0;

    private long timestamp;
    private long transactionId;

    public DefaultAsyncRow() {
        this(NO_TIMESTAMP, NO_TRANSACTION_ID);
    }

    public DefaultAsyncRow(long timestamp, long transactionId) {
        this.timestamp = timestamp;
        this.transactionId = transactionId;
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public long transactionId() {
        return transactionId;
    }

    @Override
    public void resetTransaction(@NonNull T status) {
        this.transactionId = NO_TRANSACTION_ID;
    }

    @Override
    public void startTransaction() {
        transactionId = newTransactionId();
    }

    @Override
    public boolean completable(@Nullable AsyncRow<T> query) {
        boolean newer = query == null || transactionId() == NO_TRANSACTION_ID;
        boolean response = query != null && transactionId() == query.transactionId();
        return newer || response;
    }

    @Override
    public void completeTransaction(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public void failTransaction() {
        resetTransaction(status());
    }

    private long newTransactionId() {
        return System.nanoTime();
    }
}