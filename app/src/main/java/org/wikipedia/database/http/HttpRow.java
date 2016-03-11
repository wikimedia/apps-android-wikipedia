package org.wikipedia.database.http;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public interface HttpRow {
    @NonNull
    HttpStatus status();
    long transactionId();
    long timestamp();

    void resetTransaction(@NonNull HttpStatus status);
    void startTransaction();
    boolean completeable(@Nullable HttpRow old);
    void completeTransaction(long timestamp);
}