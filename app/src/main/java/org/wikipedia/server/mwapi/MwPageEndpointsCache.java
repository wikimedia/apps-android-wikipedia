package org.wikipedia.server.mwapi;

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

/**
 * It's good to cache the Retrofit web service since it's a memory intensive object.
 * Keep the same instance around as long as we're dealing with the same domain.
 */
public final class MwPageEndpointsCache {
    public static final MwPageEndpointsCache INSTANCE = new MwPageEndpointsCache();

    private Site site;
    private MwPageService.MwPageEndpoints cachedWebService;

    private MwPageEndpointsCache() {
    }

    public MwPageService.MwPageEndpoints getMwPageEndpoints(Site newSite) {
        if (!newSite.equals(site)) {
            cachedWebService = createMwService(newSite);
            site = newSite;
        }
        return cachedWebService;
    }

    private MwPageService.MwPageEndpoints createMwService(final Site site) {
        MwPageService.MwPageEndpoints webService;
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

                .setEndpoint(WikipediaApp.getInstance().getNetworkProtocol() + "://" + domain)

                        // following is only needed for the hacky PageLead.Protection deserialization
                        // remove once https://phabricator.wikimedia.org/T69054 is resolved
                .setConverter(new GsonConverter(new GsonBuilder()
                        .registerTypeAdapter(Protection.class, new Protection.Deserializer())
                        .create()))

                .build();
        webService = restAdapter.create(MwPageService.MwPageEndpoints.class);
        return webService;
    }
}
