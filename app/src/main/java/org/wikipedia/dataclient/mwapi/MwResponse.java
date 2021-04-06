package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.json.PostProcessingTypeAdapter;

import java.util.List;

public abstract class MwResponse implements PostProcessingTypeAdapter.PostProcessable {
    @SuppressWarnings({"unused"}) @Nullable private List<MwServiceError> errors;
    @SuppressWarnings("unused,NullableProblems") @SerializedName("servedby") @NonNull private String servedBy;

    @Override
    public void postProcess() {
        if (errors != null && !errors.isEmpty()) {
            for (MwServiceError error : errors) {
                // prioritize "blocked" errors over others.
                if (error.getTitle().contains("blocked")) {
                    throw new MwException(error);
                }
            }
            throw new MwException(errors.get(0));
        }
    }
}
