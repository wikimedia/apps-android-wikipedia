package org.wikipedia.useroption.database;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.database.async.AsyncRow;
import org.wikipedia.database.http.HttpRow;
import org.wikipedia.database.http.HttpStatus;
import org.wikipedia.useroption.UserOption;

public class UserOptionRow extends UserOption implements AsyncRow<HttpStatus> {
    public static final UserOptionDatabaseTable DATABASE_TABLE = new UserOptionDatabaseTable();

    @NonNull private final HttpRow http;

    public UserOptionRow(@NonNull String key) {
        super(key);
        this.http = new HttpRow();
    }

    public UserOptionRow(@NonNull UserOption option) {
        super(option);
        this.http = new HttpRow();
    }

    public UserOptionRow(@NonNull String key, @Nullable String val) {
        super(key, val);
        this.http = new HttpRow();
    }

    public UserOptionRow(@NonNull String key, @Nullable String val, @NonNull HttpRow http) {
        super(key, val);
        this.http = http;
    }

    @NonNull @Override public HttpStatus status() {
        return http.status();
    }

    @Override
    public int statusCode() {
        return http.statusCode();
    }

    @Override
    public long timestamp() {
        return http.timestamp();
    }

    @Override
    public long transactionId() {
        return http.transactionId();
    }

    @Override
    public void resetTransaction(@NonNull HttpStatus status) {
        http.resetTransaction(status);
    }

    @Override
    public void startTransaction() {
        http.startTransaction();
    }

    @Override
    public boolean completeable(@Nullable AsyncRow<HttpStatus> old) {
        return http.completeable(old);
    }

    @Override
    public void completeTransaction(long timestamp) {
        http.completeTransaction(timestamp);
    }
}