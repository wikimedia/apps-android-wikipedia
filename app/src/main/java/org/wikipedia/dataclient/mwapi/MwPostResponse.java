package org.wikipedia.dataclient.mwapi;

import android.support.annotation.Nullable;

import org.wikipedia.server.mwapi.MwServiceError;

public abstract class MwPostResponse {
    @SuppressWarnings("unused") @Nullable private String servedby;
    @SuppressWarnings("unused") @Nullable private MwServiceError error;

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