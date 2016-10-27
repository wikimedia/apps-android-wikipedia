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

    @Nullable List<LangLink> langLinks() {
        return langlinks;
    }

    @Nullable public List<Revision> revisions() {
        return revisions;
    }

    public static class Revision {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String contentformat;
        @SuppressWarnings("unused,NullableProblems") @NonNull private String contentmodel;
        @SerializedName("*") @SuppressWarnings("unused,NullableProblems") @NonNull private String content;
        @NonNull public String content() {
            return content;
        }
    }

    static class LangLink {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String lang;
        @NonNull String lang() {
            return lang;
        }

        @SerializedName("*") @SuppressWarnings("unused,NullableProblems") @NonNull private String localizedTitle;
        @NonNull String localizedTitle() {
            return localizedTitle;
        }
    }
}
