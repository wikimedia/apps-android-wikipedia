package org.wikipedia.dataclient.mwapi;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class MwApiException extends RuntimeException {
    @SuppressWarnings("unused") @NonNull private final MwServiceError error;

    public MwApiException(@NonNull MwServiceError error) {
        this.error = error;
    }

    @Nullable String getTitle() {
        return error.getTitle();
    }

    @Override @Nullable public String getMessage() {
        return error.getDetails();
    }
}