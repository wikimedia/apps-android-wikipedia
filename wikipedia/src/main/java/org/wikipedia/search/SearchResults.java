package org.wikipedia.search;

import org.wikipedia.page.PageTitle;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple Data Object to hold search result data for both prefix search and full text search.
 */
public class SearchResults {
    private List<PageTitle> pageTitles;
    private ContinueOffset continueOffset;
    private String suggestion;

    /**
     * Empty results. Use for fallback when something goes wrong.
     */
    public SearchResults() {
        pageTitles = new ArrayList<>();
        continueOffset = null;
        suggestion = "";
    }

    /**
     * Constructor for when we get results.
     * @param pageTitles the actual results
     * @param continueOffset for search continuation
     * @param suggestion a search suggestion to show to the user: "Did you mean ...?"
     */
    public SearchResults(List<PageTitle> pageTitles,
                         ContinueOffset continueOffset,
                         String suggestion) {
        this.pageTitles = pageTitles;
        this.continueOffset = continueOffset;
        this.suggestion = suggestion;
    }

    public List<PageTitle> getPageTitles() {
        return pageTitles;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public ContinueOffset getContinueOffset() {
        return continueOffset;
    }


    /**
     * Continuation markers to pass around between search requests.
     * All internal to the actual search type, so see implementation.
     */
    public static class ContinueOffset {

    }
}

