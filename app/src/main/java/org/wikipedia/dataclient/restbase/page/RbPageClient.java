package org.wikipedia.dataclient.restbase.page;


import android.support.annotation.NonNull;

import com.google.gson.JsonParseException;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.ServiceError;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageClient;
import org.wikipedia.dataclient.page.PageLead;
import org.wikipedia.dataclient.page.PageRemaining;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.dataclient.restbase.RbDefinition;
import org.wikipedia.dataclient.retrofit.RetrofitException;
import org.wikipedia.settings.RbSwitch;
import org.wikipedia.zero.WikipediaZeroHandler;

import java.io.IOException;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit web service client for RESTBase Nodejs API.
 */
public class RbPageClient implements PageClient {
    private final Service service;
    private WikipediaZeroHandler responseHeaderHandler;

    public RbPageClient(final WikiSite wiki) {
        responseHeaderHandler = WikipediaApp.getInstance().getWikipediaZeroHandler();
        service = RbPageServiceCache.INSTANCE.getService(wiki);
    }

    @Override
    public void pageSummary(final String title, final PageSummary.Callback cb) {
        Call<RbPageSummary> call = service.pageSummary(title);
        call.enqueue(new Callback<RbPageSummary>() {
            /**
             * Invoked for a received HTTP response.
             * <p/>
             * Note: An HTTP response may still indicate an application-level failure such as a 404 or 500.
             * Call {@link Response#isSuccessful()} to determine if the response indicates success.
             */
            @Override
            public void onResponse(Call<RbPageSummary> call, Response<RbPageSummary> response) {
                if (response.isSuccessful()) {
                    responseHeaderHandler.onHeaderCheck(response);
                    if (response.body() == null) {
                        cb.failure(new JsonParseException("Response missing required field(s)"));
                        return;
                    }
                    cb.success(response.body());
                } else {
                    Throwable throwable = RetrofitException.httpError(response);
                    RbSwitch.INSTANCE.onRbRequestFailed(throwable);
                    cb.failure(throwable);
                }
            }

            /**
             * Invoked when a network exception occurred talking to the server or when an unexpected
             * exception occurred creating the request or processing the response.
             */
            @Override
            public void onFailure(Call<RbPageSummary> call, Throwable t) {
                RbSwitch.INSTANCE.onRbRequestFailed(t);
                cb.failure(t);
            }
        });
    }

    @Override
    public void pageLead(String title, final int leadImageThumbWidth, boolean noImages,
                         final PageLead.Callback cb) {
        Call<RbPageLead> call = service.pageLead(title, optional(noImages));
        call.enqueue(new Callback<RbPageLead>() {
            @Override
            public void onResponse(Call<RbPageLead> call, Response<RbPageLead> response) {
                if (response.isSuccessful()) {
                    responseHeaderHandler.onHeaderCheck(response);
                    RbPageLead pageLead = response.body();
                    pageLead.setLeadImageThumbWidth(leadImageThumbWidth);
                    cb.success(pageLead);
                } else {
                    Throwable throwable = RetrofitException.httpError(response);
                    RbSwitch.INSTANCE.onRbRequestFailed(throwable);
                    cb.failure(throwable);
                }
            }

            @Override
            public void onFailure(Call<RbPageLead> call, Throwable t) {
                RbSwitch.INSTANCE.onRbRequestFailed(t);
                cb.failure(t);
            }
        });
    }

    @Override
    public void pageRemaining(String title, boolean noImages, final PageRemaining.Callback cb) {
        Call<RbPageRemaining> call = service.pageRemaining(title, optional(noImages));
        call.enqueue(new Callback<RbPageRemaining>() {
            @Override
            public void onResponse(Call<RbPageRemaining> call, Response<RbPageRemaining> response) {
                if (response.isSuccessful()) {
                    cb.success(response.body());
                } else {
                    Throwable throwable = RetrofitException.httpError(response);
                    RbSwitch.INSTANCE.onRbRequestFailed(throwable);
                    cb.failure(throwable);
                }
            }

            @Override
            public void onFailure(Call<RbPageRemaining> call, Throwable t) {
                RbSwitch.INSTANCE.onRbRequestFailed(t);
                cb.failure(t);
            }
        });
    }

    @Override
    public RbPageCombo pageCombo(String title, boolean noImages) throws IOException {
        Response<RbPageCombo> rsp = service.pageCombo(title, optional(noImages)).execute();
        if (rsp.isSuccessful() && !rsp.body().hasError()) {
            return rsp.body();
        }
        ServiceError err = rsp.body() == null || rsp.body().getError() == null
                ? null
                : rsp.body().getError();
        throw new IOException(err == null ? rsp.message() : err.getDetails());
    }

    /* Not defined in the PageClient interface since the Wiktionary definition endpoint exists only
     * in the mobile content service, and does not concern the wholesale retrieval of the contents
     * of a wiki page.
     */
    public void define(String title, final DefinitionCallback cb) {
        Call<Map<String, RbDefinition.Usage[]>> call = service.define(title);
        call.enqueue(new Callback<Map<String, RbDefinition.Usage[]>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, RbDefinition.Usage[]>> call,
                                   @NonNull Response<Map<String, RbDefinition.Usage[]>> response) {
                if (response.isSuccessful()) {
                    if (response.body() == null) {
                        cb.failure(new JsonParseException("Response missing required fields"));
                        return;
                    }
                    responseHeaderHandler.onHeaderCheck(response);
                    cb.success(new RbDefinition(response.body()));
                } else {
                    Throwable throwable = RetrofitException.httpError(response);
                    RbSwitch.INSTANCE.onRbRequestFailed(throwable);
                    cb.failure(throwable);
                }
            }

            @Override
            public void onFailure(Call<Map<String, RbDefinition.Usage[]>> call, Throwable throwable) {
                RbSwitch.INSTANCE.onRbRequestFailed(throwable);
                cb.failure(throwable);
            }
        });
    }

    public interface DefinitionCallback {
        void success(@NonNull RbDefinition definition);

        void failure(@NonNull Throwable throwable);
    }

    /**
     * Optional boolean Retrofit parameter.
     * We don't want to send the query parameter at all when it's false since the presence of the
     * parameter alone is enough to trigger the truthy behavior.
     */
    private Boolean optional(boolean param) {
        if (param) {
            return true;
        }
        return null;
    }

    /**
     * Retrofit service for mobile content service endpoints.
     */
    interface Service {
        String ACCEPT_HEADER_MOBILE_SECTIONS = "accept: application/json; charset=utf-8; "
                + "profile=\"https://www.mediawiki.org/wiki/Specs/mobile-sections/0.8.0\"";
        String ACCEPT_HEADER_DEFINITION = "accept: application/json; charset=utf-8; "
                + "profile=\"https://www.mediawiki.org/wiki/Specs/definition/0.7.0\"";

        /**
         * Gets a page summary for a given title -- for link previews
         *
         * @param title the page title to be used including prefix
         */
        @Headers("x-analytics: preview=1")
        @GET("page/summary/{title}")
        Call<RbPageSummary> pageSummary(@Path("title") String title);

        /**
         * Gets the lead section and initial metadata of a given title.
         *
         * @param title the page title with prefix if necessary
         * @param noImages add the noimages flag to the request if true
         */
        @Headers({
                "x-analytics: pageview=1",
                ACCEPT_HEADER_MOBILE_SECTIONS
        })
        @GET("page/mobile-sections-lead/{title}")
        Call<RbPageLead> pageLead(@Path("title") String title, @Query("noimages") Boolean noImages);

        /**
         * Gets the remaining sections of a given title.
         *
         * @param title the page title to be used including prefix
         * @param noImages add the noimages flag to the request if true
         */
        @Headers(ACCEPT_HEADER_MOBILE_SECTIONS)
        @GET("page/mobile-sections-remaining/{title}")
        Call<RbPageRemaining> pageRemaining(@Path("title") String title,
                                            @Query("noimages") Boolean noImages);

        /**
         * Gets all page content of a given title -- for refreshing a saved page
         * Note: the only difference in the URL from #pageLead is the sections=all instead of 0.
         *
         * @param title the page title to be used including prefix
         * @param noImages add the noimages flag to the request if true
         */
        @Headers(ACCEPT_HEADER_MOBILE_SECTIONS)
        @GET("page/mobile-sections/{title}")
        Call<RbPageCombo> pageCombo(@Path("title") String title,
                                    @Query("noimages") Boolean noImages);

        /**
         * Gets selected Wiktionary content for a given title derived from user-selected text
         *
         * @param title the Wiktionary page title derived from user-selected Wikipedia article text
         */
        @Headers(ACCEPT_HEADER_DEFINITION)
        @GET("page/definition/{title}")
        Call<Map<String, RbDefinition.Usage[]>> define(@Path("title") String title);
    }
}
