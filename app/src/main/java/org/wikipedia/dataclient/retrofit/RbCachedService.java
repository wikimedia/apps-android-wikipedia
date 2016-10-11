package org.wikipedia.dataclient.retrofit;

import android.support.annotation.NonNull;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.server.restbase.RbPageEndpointsCache;

import retrofit2.Retrofit;

public class RbCachedService<T> extends WikiCachedService<T> {
    public RbCachedService(@NonNull Class<T> clazz) {
        super(clazz);
    }

    @NonNull @Override protected Retrofit create(@NonNull WikiSite wiki) {
        return RbPageEndpointsCache.retrofit(wiki);
    }
}