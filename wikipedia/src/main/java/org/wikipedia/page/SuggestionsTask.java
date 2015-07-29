package org.wikipedia.page;

import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.search.FullSearchArticlesTask;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.wikipedia.search.SearchResults;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.List;

/**
 * Task for getting suggestions for further reading.
 * Currently powered by full-text search based on the given page title.
 */
public class SuggestionsTask extends FullSearchArticlesTask {
    private final String title;
    private final int maxItems;
    private final boolean requireThumbnail;

    public SuggestionsTask(Api api, Site site, String title,
                           int numRequestItems, int maxResultItems,
                           int thumbSize, boolean requireThumbnail) {
        super(api, site, title, numRequestItems, null, true, thumbSize);
        this.title = title;
        this.maxItems = maxResultItems;
        this.requireThumbnail = requireThumbnail;
    }

    @Override
    public SearchResults processResult(final ApiResult result) throws Throwable {
        return filterResults(super.processResult(result));
    }

    /**
     * Filter the list of suggestions to make sure the original page title isn't one of them,
     * as well as whether the suggestion contains a thumbnail.
     *
     * @param searchResults original results from server
     * @return filtered results
     */
    public SearchResults filterResults(SearchResults searchResults) {
        final boolean verbose = WikipediaApp.getInstance().isDevRelease();
        List<PageTitle> filteredResults = new ArrayList<>();
        List<PageTitle> results = searchResults.getPageTitles();
        for (int i = 0, count = 0; i < results.size() && count < maxItems; i++) {
            final PageTitle res = results.get(i);
            if (verbose) {
                L.v(res.getPrefixedText());
            }

            if (!title.equalsIgnoreCase(res.getPrefixedText())
                    && (!requireThumbnail || res.getThumbUrl() != null)
                    && !(res.isMainPage() || res.isDisambiguationPage())) {
                filteredResults.add(res);
                count++;
            }
        }
        return new SearchResults(filteredResults, null, null);
    }
}