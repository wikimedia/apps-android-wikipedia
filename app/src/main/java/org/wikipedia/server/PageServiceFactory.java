package org.wikipedia.server;

import org.wikipedia.Site;
import org.wikipedia.server.mwapi.MwPageService;
import org.wikipedia.settings.Prefs;

/**
 * This redirection exists because we want to be able to switch between the traditional
 * MediaWiki PHP API and the new Nodejs Mobile Content Service hosted in the RESTBase
 * infrastructure.
 */
public final class PageServiceFactory {
    public static PageService create(Site site) {
        if (Prefs.isExperimentalJsonPageLoadEnabled()) {
            // Currently the same. TODO: add new RESTBase service client
            return new MwPageService(site);
        } else {
            return new MwPageService(site);
        }
    }

    private PageServiceFactory() {
    }
}
