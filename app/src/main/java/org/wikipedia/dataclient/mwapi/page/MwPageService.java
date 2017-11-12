package org.wikipedia.dataclient.mwapi.page;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.Constants;
import org.wikipedia.dataclient.okhttp.cache.SaveHeader;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Query;

/**
 * Retrofit service for MW API endpoints.
 */
public interface MwPageService {
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
    @GET("w/api.php?action=query&format=json&formatversion=2&redirects=&converttitles="
            + "&prop=extracts%7Cpageimages%7Cpageprops&exsentences=5&piprop=thumbnail%7Cname"
            + "&pilicense=any&explaintext=&pithumbsize=" + Constants.PREFERRED_THUMB_SIZE)
    @NonNull Call<MwQueryPageSummary> summary(@NonNull @Query("titles") String title);

    /**
     * Gets the lead section and initial metadata of a given title.
     *
     * @param title the page title with prefix if necessary
     * @param leadImageWidth one of the bucket widths for the lead image
     */
    @Headers("x-analytics: pageview=1")
    @GET("w/api.php?action=mobileview&format=json&formatversion=2&prop="
            + "text%7Csections%7Clanguagecount%7Cthumb%7Cimage%7Cid%7Cnamespace%7Crevision"
            + "%7Cdescription%7Clastmodified%7Cnormalizedtitle%7Cdisplaytitle%7Cprotection"
            + "%7Ceditable%7Cpageprops&pageprops=wikibase_item"
            + "&sections=0&sectionprop=toclevel%7Cline%7Canchor&noheadings=")
    @NonNull Call<MwMobileViewPageLead> lead(@Nullable @Header("Cache-Control") String cacheControl,
                                             @Header(SaveHeader.FIELD) Boolean save,
                                             @NonNull @Query("page") String title,
                                             @Query("thumbwidth") int leadImageWidth);

    /**
     * Gets the remaining sections of a given title.
     *
     * @param title the page title to be used including prefix
     */
    @GET("w/api.php?action=mobileview&format=json&formatversion=2&prop="
            + "text%7Csections&onlyrequestedsections=1&sections=1-"
            + "&sectionprop=toclevel%7Cline%7Canchor&noheadings=")
    @NonNull Call<MwMobileViewPageRemaining> sections(@Nullable @Header("Cache-Control") String cacheControl,
                                                      @Header(SaveHeader.FIELD) Boolean save,
                                                      @NonNull @Query("page") String title);
}
