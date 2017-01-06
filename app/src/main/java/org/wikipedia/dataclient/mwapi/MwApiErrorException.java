package org.wikipedia.dataclient.mwapi;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.server.mwapi.MwServiceError;

public class MwApiErrorException extends Throwable {
    @SuppressWarnings("unused") @NonNull private MwServiceError error;

    public MwApiErrorException(@NonNull MwServiceError error) {
        this.error = error;
    }

    @Nullable String getTitle() {
        return error.getTitle();
    }

    @Override @Nullable public String getMessage() {
        return error.getDetails();
    }
}
