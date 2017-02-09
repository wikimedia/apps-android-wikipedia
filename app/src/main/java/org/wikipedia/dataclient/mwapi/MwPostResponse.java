package org.wikipedia.dataclient.mwapi;

import android.support.annotation.Nullable;

public abstract class MwPostResponse extends MwResponse {
    public boolean success(@Nullable String result) {
        return super.success() && "success".equals(result);
    }

    public boolean badLoginState() {
        return "assertuserfailed".equals(code());
    }

    public boolean badToken() {
        return "badtoken".equals(code());
    }
}

