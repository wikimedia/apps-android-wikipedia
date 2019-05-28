package org.wikipedia.wikidata;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.mwapi.MwResponse;

import java.util.Collections;
import java.util.Map;

@SuppressWarnings("unused")
public class Entities extends MwResponse {
    @Nullable private Map<String, Entity> entities;
    private int success;

    @Nullable public Map<String, Entity> entities() {
        return entities;
    }

    @Nullable public Entity getFirst() {
        if (entities == null) {
            return null;
        }
        return entities.values().iterator().next();
    }

    public static class Entity {
        @Nullable private String type;
        @Nullable private String id;
        @Nullable private Map<String, Label> labels;
        @Nullable private Map<String, Label> descriptions;
        @Nullable private Map<String, SiteLink> sitelinks;

        @NonNull public String id() {
            return StringUtils.defaultString(id);
        }

        @NonNull public Map<String, Label> labels() {
            return labels != null ? labels : Collections.emptyMap();
        }

        @NonNull public Map<String, Label> descriptions() {
            return descriptions != null ? descriptions : Collections.emptyMap();
        }

        @NonNull public Map<String, SiteLink> sitelinks() {
            return sitelinks != null ? sitelinks : Collections.emptyMap();
        }
    }

    public static class Label {
        @Nullable private String language;
        @Nullable private String value;

        @NonNull public String language() {
            return StringUtils.defaultString(language);
        }

        @NonNull public String value() {
            return StringUtils.defaultString(value);
        }
    }

    public static class SiteLink {
        @Nullable private String site;
        @Nullable private String title;

        @NonNull public String getSite() {
            return StringUtils.defaultString(site);
        }

        @NonNull public String getTitle() {
            return StringUtils.defaultString(title);
        }
    }
}
