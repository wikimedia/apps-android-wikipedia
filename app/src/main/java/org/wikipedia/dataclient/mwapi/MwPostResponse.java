package org.wikipedia.dataclient.mwapi;

import android.support.annotation.Nullable;

import org.wikipedia.server.mwapi.MwServiceError;

public abstract class MwPostResponse {
    @Nullable private String servedby;
    @Nullable private MwServiceError error;

    @Nullable public String code() {
        return error == null ? null : error.getTitle();
    }

    @Nullable public String info() {
        return error == null ? null : error.getDetails();
    }

    public boolean success(@Nullable String result) {
        return error == null && "success".equals(result);
    }

    public boolean badToken() {
        return error != null && error.badToken();
    }
}