package org.wikipedia.dataclient.mwapi;

import android.support.annotation.Nullable;

import org.wikipedia.model.BaseModel;

public abstract class MwResponse extends BaseModel {
    @SuppressWarnings("unused") @Nullable private MwServiceError error;

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
}
