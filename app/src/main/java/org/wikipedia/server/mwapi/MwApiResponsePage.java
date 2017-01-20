package org.wikipedia.server.mwapi;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.model.BaseModel;

import java.util.List;

/**
 * A class representing a standard page object as returned by the MediaWiki API.
 */
public class MwApiResponsePage extends BaseModel {
    @SuppressWarnings("unused") private int pageid;
    @SuppressWarnings("unused") private int ns;
    @SuppressWarnings("unused,NullableProblems") @NonNull private String title;
    @SuppressWarnings("unused") @Nullable private List<LangLink> langlinks;
    @SuppressWarnings("unused") @Nullable private List<Revision> revisions;
    @SuppressWarnings("unused") @Nullable private Thumbnail thumbnail;
    @SuppressWarnings("unused") @Nullable private Terms terms;

    @NonNull public String title() {
        return title;
    }

    @Nullable public List<LangLink> langLinks() {
        return langlinks;
    }

    @Nullable public List<Revision> revisions() {
        return revisions;
    }

    @Nullable public String thumbUrl() {
        return thumbnail != null ? thumbnail.source() : null;
    }

    @Nullable public String description() {
        return terms != null && terms.description() != null ? terms.description().get(0) : null;
    }

    public static class Revision {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String contentformat;
        @SuppressWarnings("unused,NullableProblems") @NonNull private String contentmodel;
        @SerializedName("*") @SuppressWarnings("unused,NullableProblems") @NonNull private String content;
        @NonNull public String content() {
            return content;
        }
    }

    public static class LangLink {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String lang;
        @NonNull public String lang() {
            return lang;
        }
        @SerializedName("*") @SuppressWarnings("unused,NullableProblems") @NonNull private String localizedTitle;
        @NonNull public String localizedTitle() {
            return localizedTitle;
        }
    }

    static class Terms {
        @SuppressWarnings("unused") private List<String> description;
        List<String> description() {
            return description;
        }
    }

    static class Thumbnail {
        @SuppressWarnings("unused") private String source;
        @SuppressWarnings("unused") private int width;
        @SuppressWarnings("unused") private int height;
        String source() {
            return source;
        }
    }
}
