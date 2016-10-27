package org.wikipedia.editing;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.model.BaseModel;

public class EditToken extends BaseModel {
    @SuppressWarnings("unused,NullableProblems") @NonNull private Query query;

    @VisibleForTesting EditToken(String token) {
        Tokens tokens = new Tokens(token);
        this.query = new Query(tokens);
    }

    @NonNull protected String token() {
        return query.tokens().token();
    }

    private static class Query {
        @SuppressWarnings("unused,NullableProblems") @NonNull private Tokens tokens;
        @NonNull protected Tokens tokens() {
            return tokens;
        }

        Query(@NonNull Tokens tokens) {
            this.tokens = tokens;
        }
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
