package org.wikipedia.server.restbase;

import org.wikipedia.OkHttpConnectionFactory;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.server.Protection;
import org.wikipedia.settings.Prefs;

import com.google.gson.GsonBuilder;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;

import java.util.Locale;

/**
 * It's good to cache the Retrofit web service since it's a memory intensive object.
 * Keep the same instance around as long as we're dealing with the same domain.
 */
public final class RbEndpointsCache {
    public static final RbEndpointsCache INSTANCE = new RbEndpointsCache();

    private Site site;
    private RbContentService.RbEndpoints cachedWebService;

    private RbEndpointsCache() {
    }

    public RbContentService.RbEndpoints getRbEndpoints(Site newSite) {
        if (!newSite.equals(site)) {
            cachedWebService = createRbService(newSite);
            site = newSite;
        }
        return cachedWebService;
    }

    private RbContentService.RbEndpoints createRbService(final Site site) {
        RbContentService.RbEndpoints webService;
        final String domain = site.getDomain();
        final WikipediaApp app = WikipediaApp.getInstance();
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setLogLevel(Prefs.getRetrofitLogLevel())

                .setClient(new OkClient(OkHttpConnectionFactory.createClient(app)))

                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        app.injectCustomHeaders(request, site);
                    }
                })

                .setEndpoint(String.format(Locale.ROOT, Prefs.getRestbaseUriFormat(),
                        WikipediaApp.getInstance().getNetworkProtocol(), domain))

                        // following is only needed for the hacky PageLead.Protection deserialization
                        // remove once our service handles this better (see T111131)
                .setConverter(new GsonConverter(new GsonBuilder()
                        .registerTypeAdapter(Protection.class, new Protection.Deserializer())
                        .create()))

                .build();
        webService = restAdapter.create(RbContentService.RbEndpoints.class);
        return webService;
    }
}
