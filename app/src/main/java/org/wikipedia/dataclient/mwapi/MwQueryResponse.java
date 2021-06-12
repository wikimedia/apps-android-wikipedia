package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.Map;

import kotlinx.serialization.Serializable;

@Serializable
public class MwQueryResponse extends MwResponse {

    @SuppressWarnings("unused") @SerializedName("batchcomplete") private boolean batchComplete;

    @SuppressWarnings("unused") @SerializedName("continue") @Nullable private Map<String, String> continuation;

    @Nullable private MwQueryResult query;

    public boolean batchComplete() {
        return batchComplete;
    }

    @NonNull public Map<String, String> continuation() {
        return continuation != null ? continuation : Collections.emptyMap();
    }

    @Nullable public MwQueryResult query() {
        return query;
    }

    public boolean success() {
        return query != null;
    }

    @VisibleForTesting protected void setQuery(@Nullable MwQueryResult query) {
        this.query = query;
    }
}
