package org.wikipedia.wikidata;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.mwapi.MwResponse;

import java.util.Map;

class Entities extends MwResponse {
    @SuppressWarnings("unused") @Nullable private Map<String, Entity> entities;
    @SuppressWarnings("unused") private int success;

    @Nullable Map<String, Entity> entities() {
        return entities;
    }

    static class Entity {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String type;
        @SuppressWarnings("unused,NullableProblems") @NonNull private String id;
        @SuppressWarnings("unused,NullableProblems") @NonNull private Map<String, Label> labels;

        @NonNull public String id() {
            return id;
        }

        @NonNull Map<String, Label> labels() {
            return labels;
        }
    }

    static class Label {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String language;
        @SuppressWarnings("unused,NullableProblems") @NonNull private String value;

        @NonNull public String language() {
            return language;
        }

        @NonNull public String value() {
            return value;
        }
    }
}
