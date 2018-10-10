package org.wikipedia.dataclient.mwapi;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.json.PostProcessingTypeAdapter;
import org.wikipedia.model.BaseModel;

import java.util.Map;

public abstract class MwResponse extends BaseModel implements PostProcessingTypeAdapter.PostProcessable {
    @SuppressWarnings("unused") @Nullable private MwServiceError error;

    @SuppressWarnings("unused") @Nullable private Map<String, Warning> warnings;

    @SuppressWarnings("unused,NullableProblems") @SerializedName("servedby") @NonNull
    private String servedBy;

    private class Warning {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String warnings;
    }

    @Override
    public void postProcess() {
        if (error != null) {
            throw new MwException(error);
        }
    }
}
