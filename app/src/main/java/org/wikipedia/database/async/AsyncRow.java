package org.wikipedia.database.async;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.model.BaseModel;
import org.wikipedia.model.EnumCode;

public class AsyncRow<Status extends EnumCode, Dat> extends BaseModel {
    @Nullable private final Dat dat;
    @NonNull private final String key;
    @NonNull private Status status;
    private long timestamp;
    private long transactionId;

    public AsyncRow(@NonNull String key, @NonNull Status status, @Nullable Dat dat) {
        this(key, status, AsyncConstant.NO_TIMESTAMP, AsyncConstant.NO_TRANSACTION_ID, dat);
    }

    public AsyncRow(@NonNull AsyncRow<Status, Dat> row, @Nullable Dat dat) {
        this(row.key, row.status, row.timestamp, row.transactionId, dat);
    }

    public AsyncRow(@NonNull String key, @NonNull Status status, long timestamp, long transactionId) {
        this(key, status, timestamp, transactionId, null);
    }

    public AsyncRow(@NonNull String key, @NonNull Status status, long timestamp, long transactionId,
                    @Nullable Dat dat) {
        this.key = key;
        this.status = status;
        this.timestamp = timestamp;
        this.transactionId = transactionId;
        this.dat = dat;
    }

    @NonNull public String key() {
        return key;
    }

    @NonNull public Status status() {
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

    @Nullable public Dat dat() {
        return dat;
    }

    public void resetTransaction(@NonNull Status status) {
        this.status = status;
        this.transactionId = AsyncConstant.NO_TRANSACTION_ID;
    }

    public void startTransaction() {
        transactionId = newTransactionId();
    }

    public boolean completable(@Nullable AsyncRow<Status, Dat> query) {
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
