package org.wikipedia.dataclient.retrofit;

import android.support.annotation.NonNull;

import org.wikipedia.settings.Prefs;

import java.util.Locale;

import retrofit2.Retrofit;

public class RbCachedService<T> extends WikiCachedService<T> {
    public RbCachedService(@NonNull Class<T> clazz) {
        super(clazz);
    }

    @NonNull @Override protected Retrofit create() {
        String endpoint = String.format(Locale.ROOT, Prefs.getRestbaseUriFormat(), wiki().scheme(),
                wiki().authority());
        return RetrofitFactory.newInstance(endpoint, wiki());
    }
}
