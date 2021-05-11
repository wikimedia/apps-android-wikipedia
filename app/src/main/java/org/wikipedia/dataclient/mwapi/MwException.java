package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;

public class MwException extends RuntimeException {
    @SuppressWarnings("unused") @NonNull private final MwServiceError error;

    public MwException(@NonNull MwServiceError error) {
        this.error = error;
    }

    @NonNull public MwServiceError getError() {
        return error;
    }

    @NonNull public String getTitle() {
        return error.getTitle();
    }

    @Override @NonNull public String getMessage() {
        return error.getDetails();
    }
}
