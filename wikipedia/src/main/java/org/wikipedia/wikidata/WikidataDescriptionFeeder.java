package org.wikipedia.wikidata;

import org.wikipedia.PageTitle;
import org.wikipedia.WikipediaApp;

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
     * Adds new description entries for the titles in titleList to our shared WikidataCache.
     */
    public static void retrieveWikidataDescriptions(List<PageTitle> titleList, final WikipediaApp app,
                                                    final WikidataCache.OnWikidataReceiveListener listener) {
        List<org.wikipedia.PageTitle> pageTitles = new ArrayList<org.wikipedia.PageTitle>();
        final WikidataCache wikidataCache = app.getWikidataCache();
        for (PageTitle pageTitle : titleList) {
            if (pageTitle.getDescription() == null) {
                pageTitles.add(pageTitle);
            }
        }
        wikidataCache.get(pageTitles, listener);
    }
}
