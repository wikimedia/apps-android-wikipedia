package org.wikipedia.database.http;

import android.support.annotation.NonNull;

import org.wikipedia.database.async.AsyncColumns;

public class HttpColumns extends AsyncColumns<HttpStatus> {
    public HttpColumns(@NonNull String namePrefix) {
        super(namePrefix);
    }

    @NonNull @Override protected HttpStatus statusOf(int code) {
        return HttpStatus.of(code);
    }
}