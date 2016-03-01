package org.wikipedia.server.restbase;

import org.wikipedia.Site;
import org.wikipedia.dataclient.RestAdapterFactory;
import org.wikipedia.settings.Prefs;

import java.util.Locale;

/**
 * It's good to cache the Retrofit web service since it's a memory intensive object.
 * Keep the same instance around as long as we're dealing with the same domain.
 */
public final class RbPageEndpointsCache {
    public static final RbPageEndpointsCache INSTANCE = new RbPageEndpointsCache();

    private Site site;
    private RbPageService.RbEndpoints cachedWebService;

    private RbPageEndpointsCache() {
    }

    public RbPageService.RbEndpoints getRbEndpoints(Site newSite) {
        if (!newSite.equals(site)) {
            cachedWebService = createRbService(newSite);
            site = newSite;
        }
        return cachedWebService;
    }

    private RbPageService.RbEndpoints createRbService(Site site) {
        return RestAdapterFactory.newInstance(site,
                String.format(Locale.ROOT, Prefs.getRestbaseUriFormat(), site.scheme(), site.authority()))
                .create(RbPageService.RbEndpoints.class);
    }
}
