package org.wikipedia.page.fetch;

import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageTitle;
import org.wikipedia.settings.Prefs;

/**
 * Chooses between different variants of page loading mechanisms for the lead section.
 */
public final class LeadSectionFetcherFactory {
    public static LeadSectionFetcher create(WikipediaApp app, PageTitle title) {
        boolean downloadImages = app.isImageDownloadEnabled();
        if (Prefs.isRESTBaseJsonPageLoadEnabled()) {
            return new LeadSectionFetcherRB(title, "0", downloadImages);
        } else {
            return new LeadSectionFetcherPHP(title, "0", downloadImages);
        }
    }

    private LeadSectionFetcherFactory() {
    }
}
