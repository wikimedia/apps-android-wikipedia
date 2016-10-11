package org.wikipedia.server;

import android.support.annotation.NonNull;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.Namespace;
import org.wikipedia.server.mwapi.MwPageService;
import org.wikipedia.server.restbase.RbPageService;
import org.wikipedia.settings.RbSwitch;

/**
 * This redirection exists because we want to be able to switch between the traditional
 * MediaWiki PHP API and the new Nodejs Mobile Content Service hosted in the RESTBase
 * infrastructure.
 */
public final class PageServiceFactory {
    // TODO: remove the namespace check if and when Parsoid's handling of File pages is updated
    // T135242
    public static PageService create(@NonNull WikiSite wiki, @NonNull Namespace namespace) {
        if (RbSwitch.INSTANCE.isRestBaseEnabled(wiki) && !namespace.file()) {
            return new RbPageService(wiki);
        } else {
            return new MwPageService(wiki);
        }
    }

    private PageServiceFactory() {
    }
}
