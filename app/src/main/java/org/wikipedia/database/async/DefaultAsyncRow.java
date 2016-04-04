package org.wikipedia.database.async;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public abstract class DefaultAsyncRow<T> implements AsyncRow<T> {
    private long timestamp;
    private long transactionId;

    public DefaultAsyncRow() {
        this(AsyncConstant.NO_TIMESTAMP, AsyncConstant.NO_TRANSACTION_ID);
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
        this.transactionId = AsyncConstant.NO_TRANSACTION_ID;
    }

    @Override
    public void startTransaction() {
        transactionId = newTransactionId();
    }

    @Override
    public boolean completable(@Nullable AsyncRow<T> query) {
        boolean newer = query == null || transactionId() == AsyncConstant.NO_TRANSACTION_ID;
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

    // TODO: we should require this as a client dependency just like the timestamp.
    private long newTransactionId() {
        return System.nanoTime();
    }
}