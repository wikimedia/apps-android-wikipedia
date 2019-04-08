package org.wikipedia.wikidata;

import org.wikipedia.dataclient.mwapi.MwResponse;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Entities extends MwResponse {
    @SuppressWarnings("unused") @Nullable private Map<String, Entity> entities;
    @SuppressWarnings("unused") private int success;

    @Nullable public Map<String, Entity> entities() {
        return entities;
    }

    public static class Entity {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String type;
        @SuppressWarnings("unused,NullableProblems") @NonNull private String id;
        @SuppressWarnings("unused,NullableProblems") @NonNull private Map<String, Label> labels;
        @SuppressWarnings("unused,NullableProblems") @NonNull private Map<String, Label> descriptions;
        @SuppressWarnings("unused,NullableProblems") @NonNull private Map<String, SiteLink> sitelinks;

        @NonNull public String id() {
            return id;
        }

        @NonNull public Map<String, Label> labels() {
            return labels;
        }

        @NonNull public Map<String, Label> descriptions() {
            return descriptions;
        }

        @NonNull public Map<String, SiteLink> sitelinks() {
            return sitelinks;
        }
    }

    public static class Label {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String language;
        @SuppressWarnings("unused,NullableProblems") @NonNull private String value;

        @NonNull public String language() {
            return language;
        }

        @NonNull public String value() {
            return value;
        }
    }

    public static class SiteLink {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String site;
        @SuppressWarnings("unused,NullableProblems") @NonNull private String title;

        @NonNull public String getSite() {
            return site;
        }

        @NonNull public String getTitle() {
            return title;
        }
    }
}
