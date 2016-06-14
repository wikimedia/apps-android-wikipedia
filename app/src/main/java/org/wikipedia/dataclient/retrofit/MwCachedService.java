package org.wikipedia.dataclient.retrofit;

import android.support.annotation.NonNull;

import org.wikipedia.Site;
import org.wikipedia.server.mwapi.MwPageEndpointsCache;

import retrofit2.Retrofit;

public class MwCachedService<T> extends SiteCachedService<T> {
    public MwCachedService(@NonNull Class<T> clazz) {
        super(clazz);
    }

    @NonNull @Override protected Retrofit create(@NonNull Site site) {
        return MwPageEndpointsCache.retrofit(site);
    }
}