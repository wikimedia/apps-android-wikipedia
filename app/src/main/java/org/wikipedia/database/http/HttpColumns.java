package org.wikipedia.database.http;

import android.database.Cursor;
import android.support.annotation.NonNull;

import org.wikipedia.database.async.AsyncColumns;

public class HttpColumns extends AsyncColumns<HttpStatus> {
    public HttpColumns(@NonNull String namePrefix) {
        super(namePrefix);
    }

    @NonNull @Override public HttpRow val(@NonNull Cursor cursor) {
        return new HttpRow(status(cursor), timestamp(cursor), transactionId(cursor));
    }

    @NonNull @Override protected HttpStatus statusOf(int code) {
        return HttpStatus.of(code);
    }
}