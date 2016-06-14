package org.wikipedia.dataclient.retrofit;

import android.support.annotation.NonNull;

import org.wikipedia.Site;
import org.wikipedia.server.restbase.RbPageEndpointsCache;

import retrofit2.Retrofit;

public class RbCachedService<T> extends SiteCachedService<T> {
    public RbCachedService(@NonNull Class<T> clazz) {
        super(clazz);
    }

    @NonNull @Override protected Retrofit create(@NonNull Site site) {
        return RbPageEndpointsCache.retrofit(site);
    }
}