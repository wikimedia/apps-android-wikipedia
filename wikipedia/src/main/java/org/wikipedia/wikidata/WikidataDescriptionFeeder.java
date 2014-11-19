package org.wikipedia.wikidata;

import org.wikipedia.WikipediaApp;
import org.wikipedia.search.FullSearchResult;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility to add more entries to our shared WikidataCache.
 */
public class WikidataDescriptionFeeder {
    // do not instantiate!
    private WikidataDescriptionFeeder() {
    }

    /**
     * Adds new description entries to our shared WikidataCache.
     */
    public static void retrieveWikidataDescriptions(List<FullSearchResult> results, final WikipediaApp app,
                                                    final WikidataCache.OnWikidataReceiveListener listener) {
        List<String> wikiDataIds = new ArrayList<String>();
        final WikidataCache wikidataCache = app.getWikidataCache();
        for (FullSearchResult r : results) {
            if (!TextUtils.isEmpty(r.getWikiBaseId())) {
                wikiDataIds.add(r.getWikiBaseId());
            }
        }
        wikidataCache.get(wikiDataIds, listener);
    }
}
