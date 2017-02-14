package org.wikipedia.csrf;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.model.BaseModel;

public class CsrfToken extends BaseModel {
    @SuppressWarnings("unused,NullableProblems") @NonNull private Tokens tokens;
    @NonNull protected Tokens tokens() {
        return tokens;
    }

    @VisibleForTesting CsrfToken(String token) {
        this.tokens = new Tokens(token);
    }

    @NonNull protected String token() {
        return tokens().token();
    }

    private static class Tokens {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String csrftoken;
        @NonNull protected String token() {
            return csrftoken;
        }

        Tokens(@NonNull String token) {
            this.csrftoken = token;
        }
    }
}
