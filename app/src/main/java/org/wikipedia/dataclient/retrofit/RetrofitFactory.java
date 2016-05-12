package org.wikipedia.dataclient.retrofit;

import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.server.Protection;
import org.wikipedia.settings.Prefs;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitFactory {
    public static Retrofit newInstance(@NonNull Site site) {
        return newInstance(site, site.url() + "/");
    }

    public static Retrofit newInstance(@NonNull final Site site, @NonNull String endpoint) {
        final WikipediaApp app = WikipediaApp.getInstance();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(Prefs.getRetrofitLogLevel());

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
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
                    }
            );

        OkHttpClient client = httpClient.build();
        return new Retrofit.Builder()
                .client(client)
                .baseUrl(endpoint)
                .addConverterFactory(buildCustomGsonConverter())
                .build();
    }

    /**
     * Add custom deserializer, which is only needed for the hacky PageLead.Protection
     * deserialization.
     * Replace this method with GsonConverterFactory.create() once
     * https://phabricator.wikimedia.org/T69054 is resolved (see T111131).
     */
    private static GsonConverterFactory buildCustomGsonConverter() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Protection.class, new Protection.Deserializer());
        Gson myGson = gsonBuilder.create();
        return GsonConverterFactory.create(myGson);
    }

    private RetrofitFactory() { }
}
