package org.wikipedia.server.restbase;

import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.RestAdapterFactory;
import org.wikipedia.settings.Prefs;

import java.util.Locale;

/**
 * It's good to cache the Retrofit web service since it's a memory intensive object.
 * Keep the same instance around as long as we're dealing with the same domain.
 */
public final class RbEndpointsCache {
    public static final RbEndpointsCache INSTANCE = new RbEndpointsCache();

    private Site site;
    private RbContentService.RbEndpoints cachedWebService;

    private RbEndpointsCache() {
    }

    public RbContentService.RbEndpoints getRbEndpoints(Site newSite) {
        if (!newSite.equals(site)) {
            cachedWebService = createRbService(newSite);
            site = newSite;
        }
        return cachedWebService;
    }

    private RbContentService.RbEndpoints createRbService(Site site) {
        RbContentService.RbEndpoints webService = RestAdapterFactory.newInstance(site,
                String.format(Locale.ROOT, Prefs.getRestbaseUriFormat(),
                        WikipediaApp.getInstance().getNetworkProtocol(), site.getDomain()))
                .create(RbContentService.RbEndpoints.class);
        return webService;
    }
}
