package org.wikipedia.search;

import android.support.annotation.NonNull;

import org.wikipedia.Constants;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.MwApiResultPage;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple Data Object to hold search result data for both prefix search and full text search.
 */
public class SearchResults {
    private List<SearchResult> results;
    private ContinueOffset continueOffset;
    private String suggestion;

    /**
     * Empty results. Use for fallback when something goes wrong.
     */
    public SearchResults() {
        results = new ArrayList<>();
        continueOffset = null;
        suggestion = "";
    }

    /**
     * Constructor for a list of Retrofit results from BecauseYouReadTask.
     */
    public SearchResults(List<MwApiResultPage> pages, Site site) {
        List<SearchResult> searchResults = new ArrayList<>();
        for (MwApiResultPage page : pages) {
            searchResults.add(page.toSearchResult(site));
        }
        this.results = searchResults;
        this.continueOffset = null;
        this.suggestion = "";
    }

    /**
     * Constructor for when we get results.
     * @param results the actual results
     * @param continueOffset for search continuation
     * @param suggestion a search suggestion to show to the user: "Did you mean ...?"
     */
    public SearchResults(List<SearchResult> results,
                         ContinueOffset continueOffset,
                         String suggestion) {
        this.results = results;
        this.continueOffset = continueOffset;
        this.suggestion = suggestion;
    }

    public List<SearchResult> getResults() {
        return results;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public ContinueOffset getContinueOffset() {
        return continueOffset;
    }

    /**
     * Filter the list of suggestions to make sure the original page title isn't one of them,
     * as well as whether the suggestion contains a thumbnail.
     *
     * @param searchResults original results from server
     * @return filtered results
     */
    @NonNull
    public static SearchResults filter(SearchResults searchResults, String title,
                                       boolean requireThumbnail) {
        final boolean verbose = WikipediaApp.getInstance().isDevRelease();
        List<SearchResult> filteredResults = new ArrayList<>();
        List<SearchResult> results = searchResults.getResults();
        for (int i = 0, count = 0; i < results.size() && count < Constants.MAX_SUGGESTION_RESULTS; i++) {
            final SearchResult res = results.get(i);
            final PageTitle pageTitle = res.getPageTitle();
            if (verbose) {
                L.v(pageTitle.getPrefixedText());
            }

            if (!title.equalsIgnoreCase(pageTitle.getPrefixedText())
                    && (!requireThumbnail || pageTitle.getThumbUrl() != null)
                    && !(pageTitle.isMainPage() || pageTitle.isDisambiguationPage())) {
                filteredResults.add(res);
                count++;
            }
        }
        return new SearchResults(filteredResults, null, null);
    }


    /**
     * Continuation markers to pass around between search requests.
     * All internal to the actual search type, so see implementation.
     */
    public static class ContinueOffset {

    }
}

