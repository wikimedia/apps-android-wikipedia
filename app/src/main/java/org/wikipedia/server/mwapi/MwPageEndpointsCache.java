package org.wikipedia.server.mwapi;

import android.support.annotation.Nullable;

import org.wikipedia.Site;
import org.wikipedia.dataclient.RestAdapterFactory;

/**
 * It's good to cache the Retrofit web service since it's a memory intensive object.
 * Keep the same instance around as long as we're dealing with the same domain.
 */
public final class MwPageEndpointsCache {
    public static final MwPageEndpointsCache INSTANCE = new MwPageEndpointsCache();

    @Nullable private Site site;
    private MwPageService.MwPageEndpoints cachedWebService;

    private MwPageEndpointsCache() {
    }

    public MwPageService.MwPageEndpoints getMwPageEndpoints(Site newSite) {
        if (!newSite.equals(site)) {
            cachedWebService = createMwService(newSite);
            site = newSite;
        }
        return cachedWebService;
    }

    private MwPageService.MwPageEndpoints createMwService(Site site) {
        return RestAdapterFactory.newInstance(site).create(MwPageService.MwPageEndpoints.class);
    }
}
