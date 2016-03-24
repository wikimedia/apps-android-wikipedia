package org.wikipedia.database.async;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public interface  AsyncRow<T> {
    @NonNull T status();
    int statusCode();
    long timestamp();
    long transactionId();

    void resetTransaction(@NonNull T status);
    void startTransaction();
    boolean completable(@Nullable AsyncRow<T> query);
    void completeTransaction(long timestamp);
    void failTransaction();
}