package org.wikipedia.dataclient.okhttp;

import android.support.annotation.NonNull;

import java.io.IOException;

import okhttp3.Response;

public class HttpStatusException extends IOException {
    private final int code;
    private final String url;

    public HttpStatusException(@NonNull Response rsp) {
        this.code = rsp.code();
        this.url = rsp.request().url().uri().toString();
    }

    public int code() {
        return code;
    }

    @Override
    public String getMessage() {
        return "Code: " + Integer.toString(code) + ", URL: " + url;
    }
}
