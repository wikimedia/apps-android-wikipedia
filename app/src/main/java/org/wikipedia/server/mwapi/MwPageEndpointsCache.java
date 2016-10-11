package org.wikipedia.server.mwapi;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.retrofit.RetrofitFactory;

import retrofit2.Retrofit;

/**
 * It's good to cache the Retrofit web service since it's a memory intensive object.
 * Keep the same instance around as long as we're dealing with the same domain.
 */
public final class MwPageEndpointsCache {
    public static final MwPageEndpointsCache INSTANCE = new MwPageEndpointsCache();

    @Nullable private WikiSite wiki;
    private MwPageService.MwPageEndpoints cachedWebService;

    public static Retrofit retrofit(@NonNull WikiSite wiki) {
        return RetrofitFactory.newInstance(wiki);
    }

    private MwPageEndpointsCache() {
    }

    public MwPageService.MwPageEndpoints getMwPageEndpoints(WikiSite newWikiSite) {
        if (!newWikiSite.equals(wiki)) {
            cachedWebService = createMwService(newWikiSite);
            wiki = newWikiSite;
        }
        return cachedWebService;
    }

    public void update() {
        if (wiki != null) {
            cachedWebService = createMwService(wiki);
        }
    }

    private MwPageService.MwPageEndpoints createMwService(WikiSite wiki) {
        return retrofit(wiki).create(MwPageService.MwPageEndpoints.class);
    }
}
