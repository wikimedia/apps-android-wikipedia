package org.wikipedia.database.http;

import android.database.Cursor;
import android.support.annotation.NonNull;

import org.wikipedia.database.async.AsyncColumns;

public class HttpColumns<T> extends AsyncColumns<HttpStatus, T, HttpRow<T>> {
    public HttpColumns(@NonNull String tbl) {
        super(tbl, "http", HttpStatus.CODE_ENUM);
    }

    @NonNull @Override public HttpRow<T> val(@NonNull Cursor cursor) {
        return new HttpRow<>(key(cursor), status(cursor), timestamp(cursor), transactionId(cursor));
    }
}
