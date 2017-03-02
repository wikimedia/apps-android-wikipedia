package org.wikipedia.dataclient.restbase.page;

import org.wikipedia.dataclient.restbase.RbDefinition;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit service for mobile content service endpoints.
 */
public interface RbPageService {
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
