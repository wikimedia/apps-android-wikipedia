package org.wikipedia.dataclient.restbase.page;

import android.support.annotation.NonNull;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.retrofit.RetrofitFactory;
import org.wikipedia.settings.Prefs;

import java.util.Locale;

import retrofit2.Retrofit;

/**
 * It's good to cache the Retrofit web service since it's a memory intensive object.
 * Keep the same instance around as long as we're dealing with the same domain.
 */
public final class RbPageServiceCache {
    public static final RbPageServiceCache INSTANCE = new RbPageServiceCache();

    private WikiSite wiki;
    private RbPageClient.Service service;
    private Retrofit retrofit;

    public static Retrofit retrofit(@NonNull WikiSite wiki) {
        String endpoint = String.format(Locale.ROOT, Prefs.getRestbaseUriFormat(), wiki.scheme(),
                wiki.authority());
        return RetrofitFactory.newInstance(endpoint, wiki);
    }

    public Retrofit getRetrofit() {
        return retrofit;
    }

    public RbPageClient.Service getService(WikiSite newWikiSite) {
        if (!newWikiSite.equals(wiki)) {
            service = createService(newWikiSite);
            wiki = newWikiSite;
        }
        return service;
    }

    public void update() {
        if (wiki != null) {
            service = createService(wiki);
        }
    }

    private RbPageClient.Service createService(WikiSite wiki) {
        retrofit = retrofit(wiki);
        return retrofit.create(RbPageClient.Service.class);
    }

    private RbPageServiceCache() { }
}
