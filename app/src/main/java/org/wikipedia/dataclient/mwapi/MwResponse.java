package org.wikipedia.dataclient.mwapi;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.model.BaseModel;

import java.util.Map;

public abstract class MwResponse extends BaseModel {
    @SuppressWarnings("unused") @Nullable private MwServiceError error;

    @SuppressWarnings("unused") @Nullable private Map<String, Warning> warnings;

    @SuppressWarnings("unused,NullableProblems") @SerializedName("servedby") @NonNull
    private String servedBy;

    @Nullable public MwServiceError getError() {
        return error;
    }

    public boolean hasError() {
        return error != null;
    }

    public boolean success() {
        return error == null;
    }

    @Nullable public String code() {
        return error != null ? error.getTitle() : null;
    }

    @Nullable public String info() {
        return error != null ? error.getDetails() : null;
    }

    public boolean badToken() {
        return error != null && error.badToken();
    }

    private class Warning {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String warnings;
    }
}
