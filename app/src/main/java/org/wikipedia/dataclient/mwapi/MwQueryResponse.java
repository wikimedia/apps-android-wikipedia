package org.wikipedia.dataclient.mwapi;

import org.wikipedia.server.mwapi.MwServiceError;

import com.google.gson.annotations.SerializedName;

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

public class MwQueryResponse<T> {
    @Nullable
    private MwServiceError error;

    @SerializedName("batchcomplete")
    private boolean batchComplete;

    @Nullable
    private T query;

    @Nullable
    public MwServiceError getError() {
        return error;
    }

    public boolean batchComplete() {
        return batchComplete;
    }

    @Nullable
    public T query() {
        return query;
    }

    public boolean success() {
        return error == null && query != null;
    }

    @VisibleForTesting
    protected void setQuery(@Nullable T query) {
        this.query = query;
    }
}