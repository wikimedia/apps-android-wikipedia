package org.wikipedia.dataclient.wikidata;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.mwapi.MwResponse;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class Search extends MwResponse {
    @Nullable private List<SearchResult> search;
    private int success;

    @NonNull public List<SearchResult> results() {
        return search != null ? search : Collections.emptyList();
    }

    public static class SearchResult {
        @Nullable private String id;
        @Nullable private String label;
        @Nullable private String description;

        @NonNull public String getId() {
            return StringUtils.defaultString(id);
        }

        @NonNull public String getLabel() {
            return StringUtils.defaultString(label);
        }

        @NonNull public String getDescription() {
            return StringUtils.defaultString(description);
        }
    }
}
