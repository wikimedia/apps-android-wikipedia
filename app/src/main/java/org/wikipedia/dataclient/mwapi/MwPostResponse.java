package org.wikipedia.dataclient.mwapi;

import androidx.annotation.Nullable;

public class MwPostResponse extends MwResponse {
    @Nullable @SuppressWarnings("unused") private String options;
    @SuppressWarnings("unused") private int success;

    public boolean success(@Nullable String result) {
        return "success".equals(result);
    }

    @Nullable public String getOptions() {
        return options;
    }

    public int getSuccessVal() {
        return success;
    }
}

