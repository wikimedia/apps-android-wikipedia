package org.wikipedia.server.restbase;

import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.server.PageCombo;
import org.wikipedia.server.PageLead;
import org.wikipedia.server.PageRemaining;
import org.wikipedia.server.PageService;
import org.wikipedia.server.PageSummary;
import org.wikipedia.settings.RbSwitch;
import org.wikipedia.zero.WikipediaZeroHandler;

import java.util.Map;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Retrofit web service client for RESTBase Nodejs API.
 */
public class RbContentService implements PageService {
    private final RbEndpoints webService;
    private WikipediaZeroHandler responseHeaderHandler;

    public RbContentService(final Site site) {
        responseHeaderHandler = WikipediaApp.getInstance().getWikipediaZeroHandler();
        webService = RbEndpointsCache.INSTANCE.getRbEndpoints(site);
    }

    @Override
    public void pageSummary(String title, final PageSummary.Callback cb) {
        webService.pageSummary(title, new Callback<RbPageSummary>() {
            @Override
            public void success(RbPageSummary pageSummary, Response response) {
                responseHeaderHandler.onHeaderCheck(response);
                cb.success(pageSummary, response);
            }

            @Override
            public void failure(RetrofitError error) {
                RbSwitch.INSTANCE.onRbRequestFailed(error);
                cb.failure(error);
            }
        });
    }

    @Override
    public void pageLead(String title, final int leadImageThumbWidth, boolean noImages,
                         final PageLead.Callback cb) {
        webService.pageLead(title, optional(noImages), new Callback<RbPageLead>() {
            @Override
            public void success(RbPageLead pageLead, Response response) {
                responseHeaderHandler.onHeaderCheck(response);
                pageLead.setLeadImageThumbWidth(leadImageThumbWidth);
                cb.success(pageLead, response);
            }

            @Override
            public void failure(RetrofitError error) {
                RbSwitch.INSTANCE.onRbRequestFailed(error);
                cb.failure(error);
            }
        });
    }

    @Override
    public void pageRemaining(String title, boolean noImages, final PageRemaining.Callback cb) {
        webService.pageRemaining(title, optional(noImages), new Callback<RbPageRemaining>() {
            @Override
            public void success(RbPageRemaining pageRemaining, Response response) {
                cb.success(pageRemaining, response);
            }

            @Override
            public void failure(RetrofitError error) {
                RbSwitch.INSTANCE.onRbRequestFailed(error);
                cb.failure(error);
            }
        });
    }

    @Override
    public void pageCombo(String title, boolean noImages, final PageCombo.Callback cb) {
        webService.pageCombo(title, optional(noImages), new Callback<RbPageCombo>() {
            @Override
            public void success(RbPageCombo pageCombo, Response response) {
                cb.success(pageCombo, response);
            }

            @Override
            public void failure(RetrofitError error) {
                RbSwitch.INSTANCE.onRbRequestFailed(error);
                cb.failure(error);
            }
        });
    }

    /* Not defined in the PageService interface since the Wiktionary definition endpoint exists only
     * in the mobile content service, and does not concern the wholesale retrieval of the contents
     * of a wiki page.
     */
    public void define(String title, final RbDefinition.Callback cb) {
        webService.define(title, new Callback<Map<String, RbDefinition.Usage[]>>() {
            @Override
            public void success(Map<String, RbDefinition.Usage[]> definition, Response response) {
                responseHeaderHandler.onHeaderCheck(response);
                cb.success(new RbDefinition(definition), response);
            }

            @Override
            public void failure(RetrofitError error) {
                RbSwitch.INSTANCE.onRbRequestFailed(error);
                cb.failure(error);
            }
        });
    }

    /**
     * Optional boolean Retrofit parameter.
     * We don't want to send the query parameter at all when it's false since the presence of the parameter
     * alone is enough to trigger the truthy behavior.
     */
    private Boolean optional(boolean param) {
        if (param) {
            return true;
        }
        return null;
    }

    /**
     * Retrofit endpoints for mobile content service endpoints.
     */
    interface RbEndpoints {
        /**
         * Gets a page summary for a given title -- for link previews
         *
         * @param title the page title to be used including prefix
         * @param cb a Retrofit callback which provides the populated RbPageCombo object in #success
         */
        @GET("/page/summary/{title}")
        void pageSummary(@Path("title") String title, Callback<RbPageSummary> cb);

        /**
         * Gets the lead section and initial metadata of a given title.
         *
         * @param title the page title with prefix if necessary
         * @param noImages add the noimages flag to the request if true
         * @param cb a Retrofit callback which provides the populated RbPageLead object in #success
         */
        @GET("/page/mobile-sections-lead/{title}")
        void pageLead(@Path("title") String title, @Query("noimages") Boolean noImages,
                      Callback<RbPageLead> cb);

        /**
         * Gets the remaining sections of a given title.
         *
         * @param title the page title to be used including prefix
         * @param noImages add the noimages flag to the request if true
         * @param cb a Retrofit callback which provides the populated RbPageRemaining object in #success
         */
        @GET("/page/mobile-sections-remaining/{title}")
        void pageRemaining(@Path("title") String title, @Query("noimages") Boolean noImages,
                           Callback<RbPageRemaining> cb);

        /**
         * Gets all page content of a given title -- for refreshing a saved page
         * Note: the only difference in the URL from #pageLead is the sections=all instead of 0.
         *
         * @param title the page title to be used including prefix
         * @param noImages add the noimages flag to the request if true
         * @param cb a Retrofit callback which provides the populated RbPageCombo object in #success
         */
        @GET("/page/mobile-sections/{title}")
        void pageCombo(@Path("title") String title, @Query("noimages") Boolean noImages,
                       Callback<RbPageCombo> cb);


        /**
         * Gets selected Wiktionary content for a given title derived from user-selected text
         *
         * @param title the Wiktionary page title derived from user-selected Wikipedia article text
         */
        @GET("/page/definition/{title}")
        void define(@Path("title") String title, Callback<Map<String, RbDefinition.Usage[]>> cb);
    }
}
