package org.wikipedia.wikidata;

import android.support.annotation.NonNull;

import org.wikipedia.dataclient.mwapi.MwResponse;
import org.wikipedia.json.annotations.Required;

import java.util.Map;

public class Entity {
    @SuppressWarnings("unused,NullableProblems") @Required @NonNull private String type;
    @SuppressWarnings("unused,NullableProblems") @Required @NonNull private String id;
    @SuppressWarnings("unused,NullableProblems") @Required @NonNull private Map<String, Label> labels;

    @NonNull public String id() {
        return id;
    }

    @NonNull public Map<String, Label> labels() {
        return labels;
    }

    static class EntitiesResponse extends MwResponse {
        @SuppressWarnings("unused,NullableProblems") @Required @NonNull private Map<String, Entity> entities;
        @SuppressWarnings("unused,NullableProblems") @Required private int success;

        @NonNull public Map<String, Entity> entities() {
            return entities;
        }
    }

    public static class Label {
        @SuppressWarnings("unused,NullableProblems") @Required @NonNull private String language;
        @SuppressWarnings("unused,NullableProblems") @Required @NonNull private String value;

        @NonNull public String language() {
            return language;
        }

        @NonNull public String value() {
            return value;
        }
    }
}
