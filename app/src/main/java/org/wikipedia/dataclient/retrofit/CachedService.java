package org.wikipedia.dataclient.retrofit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import retrofit2.Retrofit;

public abstract class CachedService<T> {
    @NonNull private final Class<T> clazz;
    @Nullable private Retrofit retrofit;
    @Nullable private T service;

    public CachedService(@NonNull Class<T> clazz) {
        this.clazz = clazz;
    }

    @Nullable protected T service() {
        return service;
    }

    @Nullable public Retrofit retrofit() {
        return retrofit;
    }

    protected void update() {
        retrofit = create();
        service = retrofit.create(clazz);
    }

    protected boolean outdated() {
        return service == null;
    }

    @NonNull protected abstract Retrofit create();
}
