package org.wikipedia.server;

import org.wikipedia.Site;
import org.wikipedia.server.mwapi.MwPageService;
import org.wikipedia.server.restbase.RbPageService;
import org.wikipedia.settings.RbSwitch;

/**
 * This redirection exists because we want to be able to switch between the traditional
 * MediaWiki PHP API and the new Nodejs Mobile Content Service hosted in the RESTBase
 * infrastructure.
 */
public final class PageServiceFactory {
    public static PageService create(Site site) {
        if (RbSwitch.INSTANCE.isRestBaseEnabled(site)) {
            return new RbPageService(site);
        } else {
            return new MwPageService(site);
        }
    }

    private PageServiceFactory() {
    }
}
