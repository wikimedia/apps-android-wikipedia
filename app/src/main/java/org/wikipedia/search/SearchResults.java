package org.wikipedia.search;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.Constants;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.ReleaseUtil;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    /**
     * Constructor for filtered results (currently also used by legacy MW API AsyncTasks).
     */
    public SearchResults(@NonNull List<SearchResult> results,
                         @Nullable Map<String, String> continuation,
                         @Nullable String suggestion) {
        this.results = results;
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

    /**
     * Filter the list of results to make sure the original page title isn't one of them
     * and the suggestions have thumbnails if required.
     *
     * @param searchResults original results from server
     * @return filtered results
     */
    @NonNull public static SearchResults filter(SearchResults searchResults, String title,
                                                boolean requireThumbnail) {
        final boolean verbose = ReleaseUtil.isDevRelease();
        List<SearchResult> filteredResults = new ArrayList<>();
        List<SearchResult> results = searchResults.getResults();
        for (int i = 0; i < results.size() && filteredResults.size() < Constants.MAX_SUGGESTION_RESULTS; i++) {
            final SearchResult res = results.get(i);
            final PageTitle pageTitle = res.getPageTitle();
            if (verbose) {
                L.v(pageTitle.getPrefixedText());
            }

            if (!title.equalsIgnoreCase(pageTitle.getPrefixedText())
                    && (!requireThumbnail || pageTitle.getThumbUrl() != null)
                    && !(pageTitle.isMainPage() || pageTitle.isDisambiguationPage())) {
                filteredResults.add(res);
            }
        }
        return new SearchResults(filteredResults, null, null);
    }
}

