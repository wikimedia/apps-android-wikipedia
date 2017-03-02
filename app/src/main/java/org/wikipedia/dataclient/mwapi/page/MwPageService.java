package org.wikipedia.dataclient.mwapi.page;

import org.wikipedia.Constants;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

/**
 * Retrofit service for MW API endpoints.
 */
interface MwPageService {
    /**
     * Gets the lead section and initial metadata of a given title.
     *
     * @param title the page title with prefix if necessary
     * @return a Retrofit Call which provides the populated MwMobileViewPageLead object in #success
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
            + "&pilicense=any&pithumbsize=" + Constants.PREFERRED_THUMB_SIZE)
    Call<MwQueryPageSummary> pageSummary(@Query("titles") String title);

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
            + "%7Ceditable%7Cpageprops&pageprops=wikibase_item&onlyrequestedsections=1"
            + "&sections=0&sectionprop=toclevel%7Cline%7Canchor&noheadings=true")
    Call<MwMobileViewPageLead> pageLead(@Query("page") String title,
                                        @Query("thumbwidth") int leadImageThumbWidth,
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
    Call<MwMobileViewPageRemaining> pageRemaining(@Query("page") String title,
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
            + "%7Cpageprops&pageprops=wikibase_item&onlyrequestedsections=1&sections=all"
            + "&sectionprop=toclevel%7Cline%7Canchor&noheadings=true")
    Call<MwMobileViewPageCombo> pageCombo(@Query("page") String title,
                                          @Query("noimages") Boolean noImages);
}
