package org.wikipedia.wikidata;

import org.wikipedia.PageTitle;
import org.wikipedia.WikipediaApp;
import org.wikipedia.search.FullSearchResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility to add more entries to our shared WikidataCache.
 */
public final class WikidataDescriptionFeeder {
    // do not instantiate!
    private WikidataDescriptionFeeder() {
    }

    /**
     * Adds new description entries to our shared WikidataCache.
     */
    public static void retrieveWikidataDescriptions(List<FullSearchResult> results, final WikipediaApp app,
                                                    final WikidataCache.OnWikidataReceiveListener listener) {
        List<PageTitle> pageTitles = new ArrayList<PageTitle>();
        final WikidataCache wikidataCache = app.getWikidataCache();
        for (FullSearchResult r : results) {
            if (r.getDescription() == null) {
                pageTitles.add(r.getTitle());
            }
        }
        wikidataCache.get(pageTitles, listener);
    }
}
