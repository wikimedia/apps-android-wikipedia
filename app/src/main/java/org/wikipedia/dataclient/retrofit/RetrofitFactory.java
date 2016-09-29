package org.wikipedia.dataclient.retrofit;

import android.support.annotation.NonNull;

import org.wikipedia.OkHttpConnectionFactory;
import org.wikipedia.Site;
import org.wikipedia.json.GsonUtil;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitFactory {
    public static Retrofit newInstance(@NonNull Site site) {
        return newInstance(site.url() + "/");
    }

    public static Retrofit newInstance(@NonNull String endpoint) {
        return new Retrofit.Builder()
                .client(OkHttpConnectionFactory.getClient())
                .baseUrl(endpoint)
                .addConverterFactory(GsonConverterFactory.create(GsonUtil.getDefaultGson()))
                .build();
    }

    private RetrofitFactory() { }
}
