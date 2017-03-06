package org.wikipedia.dataclient.page;

import android.support.annotation.NonNull;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.page.MwPageClient;
import org.wikipedia.dataclient.mwapi.page.MwPageService;
import org.wikipedia.dataclient.restbase.page.RbPageClient;
import org.wikipedia.dataclient.restbase.page.RbPageService;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.RbCachedService;
import org.wikipedia.dataclient.retrofit.WikiCachedService;
import org.wikipedia.page.Namespace;
import org.wikipedia.settings.RbSwitch;

/**
 * This redirection exists because we want to be able to switch between the traditional
 * MediaWiki PHP API and the new Nodejs Mobile Content Service hosted in the RESTBase
 * infrastructure.
 */
public final class PageClientFactory {
    @NonNull private static final WikiCachedService<RbPageService> RESTBASE_CACHE
            = new RbCachedService<>(RbPageService.class);
    @NonNull private static final WikiCachedService<MwPageService> MEDIAWIKI_CACHE
            = new MwCachedService<>(MwPageService.class);

    // TODO: remove the namespace check if and when Parsoid's handling of File pages is updated
    // T135242
    public static PageClient create(@NonNull WikiSite wiki, @NonNull Namespace namespace) {
        if (RbSwitch.INSTANCE.isRestBaseEnabled(wiki) && !namespace.file()) {
            return new RbPageClient(RESTBASE_CACHE.service(wiki));
        }
        return new MwPageClient(MEDIAWIKI_CACHE.service(wiki));
    }

    private PageClientFactory() { }
}
