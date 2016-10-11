package org.wikipedia.dataclient.retrofit;

import android.support.annotation.NonNull;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.server.mwapi.MwPageEndpointsCache;

import retrofit2.Retrofit;

public class MwCachedService<T> extends WikiCachedService<T> {
    public MwCachedService(@NonNull Class<T> clazz) {
        super(clazz);
    }

    @NonNull @Override protected Retrofit create(@NonNull WikiSite wiki) {
        return MwPageEndpointsCache.retrofit(wiki);
    }
}