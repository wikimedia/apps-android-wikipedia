package org.wikipedia.server.restbase;

import android.support.annotation.NonNull;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.retrofit.RetrofitFactory;
import org.wikipedia.settings.Prefs;

import retrofit2.Retrofit;

import java.util.Locale;

/**
 * It's good to cache the Retrofit web service since it's a memory intensive object.
 * Keep the same instance around as long as we're dealing with the same domain.
 */
public final class RbPageEndpointsCache {
    public static final RbPageEndpointsCache INSTANCE = new RbPageEndpointsCache();

    private WikiSite wiki;
    private RbPageService.RbEndpoints cachedWebService;
    private Retrofit retrofit;

    public static Retrofit retrofit(@NonNull WikiSite wiki) {
        String endpoint = String.format(Locale.ROOT, Prefs.getRestbaseUriFormat(), wiki.scheme(),
                wiki.authority());
        return RetrofitFactory.newInstance(endpoint, wiki);
    }

    private RbPageEndpointsCache() {
    }

    public Retrofit getRetrofit() {
        return retrofit;
    }

    public RbPageService.RbEndpoints getRbEndpoints(WikiSite newWikiSite) {
        if (!newWikiSite.equals(wiki)) {
            cachedWebService = createRbService(newWikiSite);
            wiki = newWikiSite;
        }
        return cachedWebService;
    }

    public void update() {
        if (wiki != null) {
            cachedWebService = createRbService(wiki);
        }
    }

    private RbPageService.RbEndpoints createRbService(WikiSite wiki) {
        retrofit = retrofit(wiki);
        return retrofit.create(RbPageService.RbEndpoints.class);
    }
}
