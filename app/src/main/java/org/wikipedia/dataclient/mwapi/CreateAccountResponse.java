package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CreateAccountResponse extends MwResponse {
    @SuppressWarnings("unused") @Nullable private Result createaccount;

    @Nullable public String status() {
        return createaccount.status();
    }

    @Nullable public String user() {
        return createaccount.user();
    }

    @Nullable public String message() {
        return createaccount.message();
    }

    public boolean hasResult() {
        return createaccount != null;
    }

    public static class Result {
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
