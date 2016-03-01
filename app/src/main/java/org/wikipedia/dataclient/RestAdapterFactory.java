package org.wikipedia.dataclient;

import android.support.annotation.NonNull;

import com.google.gson.GsonBuilder;

import org.wikipedia.OkHttpConnectionFactory;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.server.Protection;
import org.wikipedia.settings.Prefs;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

public final class RestAdapterFactory {
    public static RestAdapter newInstance(@NonNull Site site) {
        return newInstance(site, site.scheme() + "://" + site.authority());
    }

    public static RestAdapter newInstance(@NonNull final Site site, @NonNull String endpoint) {
        final WikipediaApp app = WikipediaApp.getInstance();
        return new RestAdapter.Builder()
                .setLogLevel(Prefs.getRetrofitLogLevel())
                .setClient(new NullBodyAwareOkClient(OkHttpConnectionFactory.createClient(app)))
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        app.injectCustomHeaders(request, site);
                    }
                })
                .setEndpoint(endpoint)

                // following is only needed for the hacky PageLead.Protection deserialization
                // remove once https://phabricator.wikimedia.org/T69054 is resolved (see T111131)
                .setConverter(new GsonConverter(new GsonBuilder()
                        .registerTypeAdapter(Protection.class, new Protection.Deserializer())
                        .create()))

                .build();
    }

    private RestAdapterFactory() { }
}