package org.wikipedia.server.restbase;

import org.wikipedia.OkHttpConnectionFactory;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;

import com.google.gson.GsonBuilder;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;

/**
 * It's good to cache the Retrofit web service since it's a memory intensive object.
 * Keep the same instance around as long as we're dealing with the same domain.
 */
public final class RbPageEndpointsCache {
    public static final RbPageEndpointsCache INSTANCE = new RbPageEndpointsCache();

    private Site site;
    private RbPageService.RbPageEndpoints cachedWebService;

    private RbPageEndpointsCache() {
    }

    public RbPageService.RbPageEndpoints getRbPageEndpoints(Site newSite) {
        if (!newSite.equals(site)) {
            cachedWebService = createRbService(newSite);
            site = newSite;
        }
        return cachedWebService;
    }

    private RbPageService.RbPageEndpoints createRbService(final Site site) {
        RbPageService.RbPageEndpoints webService;
        final String domain = site.getApiDomain();
        final WikipediaApp app = WikipediaApp.getInstance();
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.FULL)

                .setClient(new OkClient(OkHttpConnectionFactory.createClient(app)))

                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        app.getMccMncStateHandler().injectMccMncHeader(app, domain, request);
                        app.injectCustomHeaders(request, site);
                    }
                })

                .setEndpoint(WikipediaApp.getInstance().getNetworkProtocol() + "://" + domain)

                        // following is only needed for the hacky PageLead.Protection deserialization
                        // remove once our service handles this better (see T111131)
                .setConverter(new GsonConverter(new GsonBuilder()
                        .registerTypeAdapter(RbPageLead.Protection.class,
                                new RbPageLead.Protection.Deserializer())
                        .create()))

                .build();
        webService = restAdapter.create(RbPageService.RbPageEndpoints.class);
        return webService;
    }
}
