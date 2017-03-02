package org.wikipedia.dataclient.retrofit;

import android.support.annotation.NonNull;

import retrofit2.Retrofit;

public class MwCachedService<T> extends WikiCachedService<T> {
    public MwCachedService(@NonNull Class<T> clazz) {
        super(clazz);
    }

    @NonNull @Override protected Retrofit create() {
        return RetrofitFactory.newInstance(wiki());
    }
}
