package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;

class MwAuthManagerInfo {
    @SuppressWarnings("unused,NullableProblems") @NonNull private List<Request> requests;

    @NonNull List<Request> requests() {
        return requests;
    }

    static class Request {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String id;
        @SuppressWarnings("unused,NullableProblems") @NonNull private Map<String, String> metadata;
        @SuppressWarnings("unused,NullableProblems") @NonNull private String required;
        @SuppressWarnings("unused,NullableProblems") @NonNull private String provider;
        @SuppressWarnings("unused,NullableProblems") @NonNull private String account;
        @SuppressWarnings("unused,NullableProblems") @NonNull private Map<String, Field> fields;

        @NonNull String id() {
            return id;
        }

        @NonNull Map<String, Field> fields() {
            return fields;
        }
    }

    static class Field {
        @SuppressWarnings("unused") @Nullable private String type;
        @SuppressWarnings("unused") @Nullable private String value;
        @SuppressWarnings("unused") @Nullable private String label;
        @SuppressWarnings("unused") @Nullable private String help;
        @SuppressWarnings("unused") private boolean optional;
        @SuppressWarnings("unused") private boolean sensitive;

        @Nullable String value() {
            return value;
        }
    }
}
