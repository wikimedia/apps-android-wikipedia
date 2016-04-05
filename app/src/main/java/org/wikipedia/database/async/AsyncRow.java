package org.wikipedia.database.async;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.model.EnumCode;

public class AsyncRow<T extends EnumCode> {
    @NonNull private final String key;
    @NonNull private T status;
    private long timestamp;
    private long transactionId;

    public AsyncRow(@NonNull String key, @NonNull T status) {
        this(key, status, AsyncConstant.NO_TIMESTAMP, AsyncConstant.NO_TRANSACTION_ID);
    }

    public AsyncRow(@NonNull String key, @NonNull T status, long timestamp, long transactionId) {
        this.key = key;
        this.status = status;
        this.timestamp = timestamp;
        this.transactionId = transactionId;
    }

    @NonNull public String key() {
        return key;
    }

    @NonNull public T status() {
        return status;
    }

    public int statusCode() {
        return status.code();
    }

    public long timestamp() {
        return timestamp;
    }

    public long transactionId() {
        return transactionId;
    }

    public void resetTransaction(@NonNull T status) {
        this.status = status;
        this.transactionId = AsyncConstant.NO_TRANSACTION_ID;
    }

    public void startTransaction() {
        transactionId = newTransactionId();
    }

    public boolean completable(@Nullable AsyncRow<T> query) {
        boolean newer = query == null || transactionId() == AsyncConstant.NO_TRANSACTION_ID;
        boolean response = query != null && transactionId() == query.transactionId();
        return newer || response;
    }

    public void completeTransaction(long timestamp) {
        this.timestamp = timestamp;
    }

    public void failTransaction() {
        resetTransaction(status());
    }

    // TODO: we should require this as a client dependency just like the timestamp.
    private long newTransactionId() {
        return System.nanoTime();
    }
}