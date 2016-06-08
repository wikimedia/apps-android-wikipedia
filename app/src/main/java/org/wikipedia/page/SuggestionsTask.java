package org.wikipedia.page;

import org.wikipedia.Constants;
import org.wikipedia.Site;
import org.wikipedia.search.FullSearchArticlesTask;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.wikipedia.search.SearchResults;

/**
 * Task for getting suggestions for further reading.
 * Currently powered by full-text search based on the given page title.
 */
public class SuggestionsTask extends FullSearchArticlesTask {
    private final String title;
    private final boolean requireThumbnail;

    public SuggestionsTask(Api api, Site site, String title, boolean requireThumbnail) {
        super(api, site, title, Constants.MAX_SUGGESTION_RESULTS, null, true);
        this.title = title;
        this.requireThumbnail = requireThumbnail;
    }

    @Override
    public SearchResults processResult(final ApiResult result) throws Throwable {
        return SearchResults.filter(super.processResult(result), title, requireThumbnail);
    }
}