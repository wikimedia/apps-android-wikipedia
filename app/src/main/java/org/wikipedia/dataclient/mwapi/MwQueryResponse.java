package org.wikipedia.dataclient.mwapi;

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class MwQueryResponse<T> extends MwResponse {

    @SuppressWarnings("unused") @SerializedName("batchcomplete") private boolean batchComplete;

    @SuppressWarnings("unused") @SerializedName("continue") @Nullable private Map<String, String> continuation;

    @Nullable private T query;

    public boolean batchComplete() {
        return batchComplete;
    }

    @Nullable public Map<String, String> continuation() {
        return continuation;
    }

    @Nullable public T query() {
        return query;
    }

    @Override public boolean success() {
        return super.success() && query != null;
    }

    @VisibleForTesting protected void setQuery(@Nullable T query) {
        this.query = query;
    }

    public static class Pages {
        @SuppressWarnings("unused") private List<MwQueryPage> pages;

        public List<MwQueryPage> pages() {
            return pages;
        }
    }
}
