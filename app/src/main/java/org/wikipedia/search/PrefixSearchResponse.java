package org.wikipedia.search;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.dataclient.mwapi.MwQueryResponse;

public class PrefixSearchResponse extends MwQueryResponse {
    @SuppressWarnings("unused") @SerializedName("searchinfo") private SearchInfo searchInfo;
    @SuppressWarnings("unused") private Search search;

    @Nullable String suggestion() {
        return searchInfo != null ? searchInfo.suggestion() : null;
    }

    static class SearchInfo {
        @SuppressWarnings("unused") @Nullable private String suggestion;
        @SuppressWarnings("unused") @SerializedName("suggestionsnippet")
        @Nullable private String snippet;

        @Nullable public String suggestion() {
            return suggestion;
        }
    }

    static class Search {
        @SuppressWarnings("unused") @SerializedName("ns") private int namespace;
        @SuppressWarnings("unused") @Nullable private String title;
    }
}
