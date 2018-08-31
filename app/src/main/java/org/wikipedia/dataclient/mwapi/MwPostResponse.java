package org.wikipedia.dataclient.mwapi;

import android.support.annotation.Nullable;

public class MwPostResponse extends MwResponse {
    @Nullable @SuppressWarnings("unused") private String options;
    @SuppressWarnings("unused") private int success;

    public boolean success(@Nullable String result) {
        return super.success() && "success".equals(result);
    }

    public boolean badLoginState() {
        return "assertuserfailed".equals(code());
    }

    public boolean badToken() {
        return "badtoken".equals(code());
    }

    @Nullable public String getOptions() {
        return options;
    }

    public int getSuccessVal() {
        return success;
    }
}

