package org.wikipedia.createaccount;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.mwapi.MwResponse;

class CreateAccountResponse extends MwResponse {
    @SuppressWarnings("unused") @Nullable private Result createaccount;

    @Nullable String status() {
        return createaccount.status();
    }

    @Nullable String user() {
        return createaccount.user();
    }

    @Nullable String message() {
        return createaccount.message();
    }

    boolean hasResult() {
        return success() && createaccount != null;
    }

    static class Result {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String status;
        @SuppressWarnings("unused") @Nullable private String message;
        @SuppressWarnings("unused") @Nullable private String username;

        @NonNull public String status() {
            return status;
        }

        @Nullable public String user() {
            return username;
        }

        @Nullable public String message() {
            return message;
        }
    }
}
