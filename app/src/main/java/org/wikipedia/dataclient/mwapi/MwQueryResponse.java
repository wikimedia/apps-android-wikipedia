package org.wikipedia.dataclient.mwapi;

import com.google.gson.annotations.SerializedName;

public class MwQueryResponse<T> {
    @SerializedName("batchcomplete")
    private boolean batchComplete;
    private T query;

    public boolean batchComplete() {
        return batchComplete;
    }

    public T query() {
        return query;
    }
}