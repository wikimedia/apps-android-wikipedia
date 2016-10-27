package org.wikipedia.editing;

import android.support.annotation.NonNull;

import org.wikipedia.model.BaseModel;

public class EditToken extends BaseModel {
    @SuppressWarnings("unused,NullableProblems") @NonNull private Query query;
    @NonNull protected String token() {
        return query.tokens().token();
    }

    private static class Query {
        @SuppressWarnings("unused,NullableProblems") @NonNull private Tokens tokens;
        @NonNull protected Tokens tokens() {
            return tokens;
        }
    }

    private static class Tokens {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String csrftoken;
        @NonNull protected String token() {
            return csrftoken;
        }
    }
}
