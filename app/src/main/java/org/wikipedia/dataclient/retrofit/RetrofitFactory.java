package org.wikipedia.dataclient.retrofit;

import android.support.annotation.NonNull;

import org.wikipedia.OkHttpConnectionFactory;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.json.GsonUtil;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitFactory {
    public static Retrofit newInstance(@NonNull Site site) {
        return newInstance(site, site.url() + "/");
    }

    public static Retrofit newInstance(@NonNull final Site site, @NonNull String endpoint) {
        final WikipediaApp app = WikipediaApp.getInstance();
        OkHttpClient client = OkHttpConnectionFactory
                .createClient(WikipediaApp.getInstance())
                .addNetworkInterceptor(
                        new Interceptor() {
                            @Override
                            public Response intercept(Chain chain) throws IOException {
                                Request request = chain.request();
                                request = request.newBuilder()
                                        .headers(app.buildCustomHeaders(request, site))
                                        .build();
                                return chain.proceed(request);
                            }
                        })
                .build();
        return new Retrofit.Builder()
                .client(client)
                .baseUrl(endpoint)
                .addConverterFactory(GsonConverterFactory.create(GsonUtil.getDefaultGson()))
                .build();
    }

    private RetrofitFactory() { }
}
