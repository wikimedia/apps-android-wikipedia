package org.wikipedia.server.mwapi;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.Site;
import org.wikipedia.dataclient.retrofit.RetrofitFactory;

import retrofit2.Retrofit;

/**
 * It's good to cache the Retrofit web service since it's a memory intensive object.
 * Keep the same instance around as long as we're dealing with the same domain.
 */
public final class MwPageEndpointsCache {
    public static final MwPageEndpointsCache INSTANCE = new MwPageEndpointsCache();

    @Nullable private Site site;
    private MwPageService.MwPageEndpoints cachedWebService;

    public static Retrofit retrofit(@NonNull Site site) {
        return RetrofitFactory.newInstance(site);
    }

    private MwPageEndpointsCache() {
    }

    public MwPageService.MwPageEndpoints getMwPageEndpoints(Site newSite) {
        if (!newSite.equals(site)) {
            cachedWebService = createMwService(newSite);
            site = newSite;
        }
        return cachedWebService;
    }

    public void update() {
        if (site != null) {
            cachedWebService = createMwService(site);
        }
    }

    private MwPageService.MwPageEndpoints createMwService(Site site) {
        return retrofit(site).create(MwPageService.MwPageEndpoints.class);
    }
}
