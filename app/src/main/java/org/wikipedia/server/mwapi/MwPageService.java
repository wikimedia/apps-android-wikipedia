package org.wikipedia.server.mwapi;

import org.wikipedia.Constants;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.retrofit.RetrofitException;
import org.wikipedia.server.PageLead;
import org.wikipedia.server.PageRemaining;
import org.wikipedia.server.PageService;
import org.wikipedia.server.PageSummary;
import org.wikipedia.server.ServiceError;
import org.wikipedia.server.restbase.RbPageEndpointsCache;
import org.wikipedia.settings.RbSwitch;
import org.wikipedia.zero.WikipediaZeroHandler;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

/**
 * Retrofit web service client for MediaWiki PHP API.
 */
public class MwPageService implements PageService {
    private final MwPageEndpoints webService;
    private final Retrofit retrofit;
    private WikipediaZeroHandler responseHeaderHandler;

    public MwPageService(final Site site) {
        responseHeaderHandler = WikipediaApp.getInstance().getWikipediaZeroHandler();
        webService = MwPageEndpointsCache.INSTANCE.getMwPageEndpoints(site);
        retrofit = RbPageEndpointsCache.INSTANCE.getRetrofit();
    }

    @Override
    public void pageSummary(String title, final PageSummary.Callback cb) {
        Call<MwPageSummary> call = webService.pageSummary(title);
        call.enqueue(new Callback<MwPageSummary>() {
            /**
             * Invoked for a received HTTP response.
             * <p/>
             * Note: An HTTP response may still indicate an application-level failure such as a 404 or 500.
             * Call {@link Response#isSuccessful()} to determine if the response indicates success.
             */
            @Override
            public void onResponse(Call<MwPageSummary> call, Response<MwPageSummary> response) {
                if (response.isSuccessful()) {
                    responseHeaderHandler.onHeaderCheck(response);
                    cb.success(response.body());
                } else {
                    cb.failure(RetrofitException.httpError(response, retrofit));
                }
            }

            /**
             * Invoked when a network exception occurred talking to the server or when an unexpected
             * exception occurred creating the request or processing the response.
             */
            @Override
            public void onFailure(Call<MwPageSummary> call, Throwable t) {
                cb.failure(t);
            }
        });
    }

    @Override
    public void pageLead(String title, int leadImageThumbWidth, boolean noImages,
                         final PageLead.Callback cb) {
        Call<MwPageLead> call = webService.pageLead(title, leadImageThumbWidth, optional(noImages));
        call.enqueue(new Callback<MwPageLead>() {
            @Override
            public void onResponse(Call<MwPageLead> call, Response<MwPageLead> response) {
                if (response.isSuccessful()) {
                    responseHeaderHandler.onHeaderCheck(response);
                    cb.success(response.body());
                } else {
                    cb.failure(RetrofitException.httpError(response, retrofit));
                }
            }

            @Override
            public void onFailure(Call<MwPageLead> call, Throwable t) {
                cb.failure(t);
            }
        });
    }

    @Override
    public void pageRemaining(String title, boolean noImages, final PageRemaining.Callback cb) {
        Call<MwPageRemaining> call = webService.pageRemaining(title, optional(noImages));
        call.enqueue(new Callback<MwPageRemaining>() {
            @Override
            public void onResponse(Call<MwPageRemaining> call, Response<MwPageRemaining> response) {
                if (response.isSuccessful()) {
                    RbSwitch.INSTANCE.onMwSuccess();
                    cb.success(response.body());
                } else {
                    cb.failure(RetrofitException.httpError(response, retrofit));
                }
            }

            @Override
            public void onFailure(Call<MwPageRemaining> call, Throwable t) {
                cb.failure(t);
            }
        });
    }

    @Override
    public MwPageCombo pageCombo(String title, boolean noImages) throws IOException {
        Response<MwPageCombo> rsp = webService.pageCombo(title, noImages).execute();
        if (rsp.isSuccessful() && !rsp.body().hasError()) {
            return rsp.body();
        }
        ServiceError err = rsp.body() == null || rsp.body().getError() == null
                ? null
                : rsp.body().getError();
        throw new IOException(err == null ? rsp.message() : err.getDetails());
    }

    /**
     * Optional boolean Retrofit parameter.
     * We don't want to send the query parameter at all when it's false since the presence of the
     * alone is enough to trigger the truthy behavior.
     */
    private Boolean optional(boolean param) {
        if (param) {
            return true;
        }
        return null;
    }

    /**
     * Retrofit endpoints for MW API endpoints.
     */
    interface MwPageEndpoints {
        /**
         * Gets the lead section and initial metadata of a given title.
         *
         * @param title the page title with prefix if necessary
         * @return a Retrofit Call which provides the populated MwPageLead object in #success
         */
         /*
          Here's the rationale for this API call:
          We request 10 sentences from the lead section, and then re-parse the text using our own
          sentence parsing logic to end up with 2 sentences for the link preview. We trust our
          parsing logic more than TextExtracts because it's better-tailored to the user's
          Locale on the client side. For example, the TextExtracts extension incorrectly treats
          abbreviations like "i.e.", "B.C.", "Jr.", etc. as separate sentences, whereas our parser
          will leave those alone.

          Also, we no longer request "excharacters" from TextExtracts, since it has an issue where
          it's liable to return content that lies beyond the lead section, which might include
          unparsed wikitext, which we certainly don't want.
        */
        @Headers("x-analytics: preview=1")
        @GET("w/api.php?action=query&format=json&formatversion=2&prop=extracts%7Cpageimages"
                + "&redirects=true&exsentences=5&explaintext=true&piprop=thumbnail%7Cname"
                + "&pithumbsize=" + Constants.PREFERRED_THUMB_SIZE)
        Call<MwPageSummary> pageSummary(@Query("titles") String title);

        /**
         * Gets the lead section and initial metadata of a given title.
         *
         * @param title the page title with prefix if necessary
         * @param leadImageThumbWidth one of the bucket widths for the lead image
         * @param noImages add the noimages flag to the request if true
         */
        @Headers("x-analytics: pageview=1")
        @GET("w/api.php?action=mobileview&format=json&formatversion=2&prop="
                + "text%7Csections%7Clanguagecount%7Cthumb%7Cimage%7Cid%7Cnamespace%7Crevision"
                + "%7Cdescription%7Clastmodified%7Cnormalizedtitle%7Cdisplaytitle%7Cprotection"
                + "%7Ceditable&onlyrequestedsections=1&sections=0&sectionprop=toclevel%7Cline"
                + "%7Canchor&noheadings=true")
        Call<MwPageLead> pageLead(@Query("page") String title,
                                  @Query("thumbsize") int leadImageThumbWidth,
                                  @Query("noimages") Boolean noImages);

        /**
         * Gets the remaining sections of a given title.
         *
         * @param title the page title to be used including prefix
         * @param noImages add the noimages flag to the request if true
         */
        @GET("w/api.php?action=mobileview&format=json&prop="
                + "text%7Csections&onlyrequestedsections=1&sections=1-"
                + "&sectionprop=toclevel%7Cline%7Canchor&noheadings=true")
        Call<MwPageRemaining> pageRemaining(@Query("page") String title,
                                            @Query("noimages") Boolean noImages);

        /**
         * Gets all page content of a given title -- for refreshing a saved page
         * Note: the only difference in the URL from #pageLead is the sections=all instead of 0.
         *
         * @param title the page title to be used including prefix
         * @param noImages add the noimages flag to the request if true
         */
        @GET("w/api.php?action=mobileview&format=json&formatversion=2&prop="
                + "text%7Csections%7Clanguagecount%7Cthumb%7Cimage%7Cid%7Crevision%7Cdescription"
                + "%7Clastmodified%7Cnormalizedtitle%7Cdisplaytitle%7Cprotection%7Ceditable"
                + "&onlyrequestedsections=1&sections=all&sectionprop=toclevel%7Cline%7Canchor"
                + "&noheadings=true")
        Call<MwPageCombo> pageCombo(@Query("page") String title,
                                    @Query("noimages") Boolean noImages);
    }
}
