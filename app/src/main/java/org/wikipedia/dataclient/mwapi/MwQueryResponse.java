package org.wikipedia.dataclient.mwapi;

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class MwQueryResponse extends MwResponse {

    @SuppressWarnings("unused") @SerializedName("batchcomplete") private boolean batchComplete;

    @SuppressWarnings("unused") @SerializedName("continue") @Nullable private Map<String, String> continuation;

    @Nullable private MwQueryResult query;

    public boolean batchComplete() {
        return batchComplete;
    }

    @Nullable public Map<String, String> continuation() {
        return continuation;
    }

    @Nullable public MwQueryResult query() {
        return query;
    }

    @Override public boolean success() {
        return super.success() && query != null;
    }

    @VisibleForTesting protected void setQuery(@Nullable MwQueryResult query) {
        this.query = query;
    }


}
