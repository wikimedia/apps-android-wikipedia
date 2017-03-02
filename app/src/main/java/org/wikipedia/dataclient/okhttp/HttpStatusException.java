package org.wikipedia.dataclient.okhttp;

import android.support.annotation.NonNull;

import java.io.IOException;

import okhttp3.Response;

public class HttpStatusException extends IOException {
    private final int code;

    public HttpStatusException(@NonNull Response rsp) {
        this.code = rsp.code();
    }

    public int code() {
        return code;
    }
}
