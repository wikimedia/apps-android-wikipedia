package org.wikipedia.page.fetch;

import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageTitle;
import org.wikipedia.settings.Prefs;

/**
 * Chooses between different variants of page loading mechanisms for the remaining sections.
 */
public final class RestSectionFetcherFactory {
    public static RestSectionFetcher create(WikipediaApp app, PageTitle title) {
        boolean downloadImages = app.isImageDownloadEnabled();
        if (Prefs.isRESTBaseJsonPageLoadEnabled()) {
            return new RestSectionFetcherRB(title, "1-", downloadImages);
        } else {
            return new RestSectionFetcherPHP(title, "1-", downloadImages);
        }
    }

    private RestSectionFetcherFactory() {
    }
}
