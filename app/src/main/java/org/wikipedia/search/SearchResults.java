package org.wikipedia.search;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryPage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Simple Data Object to hold search result data for both prefix search and full text search.
 */
public class SearchResults {
    @NonNull private List<SearchResult> results;
    @Nullable private Map<String, String> continuation;
    @Nullable private String suggestion;

    /**
     * Empty results. Use for fallback when something goes wrong.
     */
    public SearchResults() {
        results = new ArrayList<>();
        continuation = null;
    }

    /**
     * Constructor for a list of MwQueryPage search query results.
     * @param pages the result pages
     * @param wiki the wiki searched
     * @param continuation info for search continuation
     * @param suggestion a search suggestion to show to the user: "Did you mean ...?"
     */
    public SearchResults(@NonNull List<MwQueryPage> pages, @NonNull WikiSite wiki,
                         @Nullable Map<String, String> continuation, @Nullable String suggestion) {
        List<SearchResult> searchResults = new ArrayList<>();

        // Sort the array based on the "index" property
        Collections.sort(pages, (a, b) -> ((Integer) a.index()).compareTo(b.index()));

        for (MwQueryPage page : pages) {
            searchResults.add(new SearchResult(page, wiki));
        }
        this.results = searchResults;
        this.continuation = continuation;
        this.suggestion = suggestion;
    }

    @NonNull public List<SearchResult> getResults() {
        return results;
    }

    @Nullable public String getSuggestion() {
        return suggestion;
    }

    @Nullable public Map<String, String> getContinuation() {
        return continuation;
    }
}

