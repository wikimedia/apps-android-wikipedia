package org.wikipedia.server;

import org.wikipedia.Site;
import org.wikipedia.server.mwapi.MwPageService;
import org.wikipedia.server.restbase.RbContentService;
import org.wikipedia.settings.RbSwitch;

/**
 * This redirection exists because we want to be able to switch between the traditional
 * MediaWiki PHP API and the new Nodejs Mobile Content Service hosted in the RESTBase
 * infrastructure.
 */
public final class ContentServiceFactory {
    public static PageService create(Site site) {
        if (RbSwitch.INSTANCE.isRestBaseEnabled(site)) {
            return new RbContentService(site);
        } else {
            return new MwPageService(site);
        }
    }

    private ContentServiceFactory() {
    }
}
