package org.wikipedia.dataclient.mwapi;

import androidx.annotation.Nullable;

@SuppressWarnings("unused")
public class MwPostResponse extends MwResponse {
    @Nullable private MwQueryPage pageinfo;
    @Nullable private String options;
    private int success;

    public boolean success(@Nullable String result) {
        return "success".equals(result);
    }

    @Nullable public String getOptions() {
        return options;
    }

    public int getSuccessVal() {
        return success;
    }

    @Nullable public MwQueryPage getPageInfo() {
        return pageinfo;
    }
}

