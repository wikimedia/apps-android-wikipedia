package org.wikipedia.dataclient.mwapi.page;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.retrofit.RetrofitFactory;

import retrofit2.Retrofit;

// todo: can this class be replaced by WikiCachedService?
/**
 * It's good to cache the Retrofit web service since it's a memory intensive object.
 * Keep the same instance around as long as we're dealing with the same domain.
 */
public final class MwPageServiceCache {
    public static final MwPageServiceCache INSTANCE = new MwPageServiceCache();

    @Nullable private WikiSite wiki;
    private MwPageService.Service service;

    public static Retrofit retrofit(@NonNull WikiSite wiki) {
        return RetrofitFactory.newInstance(wiki);
    }

    private MwPageServiceCache() {
    }

    public MwPageService.Service getService(WikiSite newWikiSite) {
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

    private MwPageService.Service createService(WikiSite wiki) {
        return retrofit(wiki).create(MwPageService.Service.class);
    }
}
