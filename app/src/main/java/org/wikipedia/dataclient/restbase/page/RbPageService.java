package org.wikipedia.dataclient.restbase.page;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.okhttp.cache.SaveHeader;
import org.wikipedia.dataclient.restbase.RbDefinition;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Path;

import static org.wikipedia.Constants.ACCEPT_HEADER_PREFIX;
import static org.wikipedia.Constants.ACCEPT_HEADER_SUMMARY;

/**
 * Retrofit service for mobile content service endpoints.
 */
public interface RbPageService {
    String ACCEPT_HEADER_MOBILE_SECTIONS = ACCEPT_HEADER_PREFIX + "mobile-sections/0.12.4\"";
    String ACCEPT_HEADER_DEFINITION = ACCEPT_HEADER_PREFIX + "definition/0.7.2\"";

    /**
     * Gets a page summary for a given title -- for link previews
     *
     * @param title the page title to be used including prefix
     */
    @Headers({
            "x-analytics: preview=1",
            ACCEPT_HEADER_SUMMARY
    })
    @GET("page/summary/{title}")
    @NonNull Call<RbPageSummary> summary(@NonNull @Path("title") String title);

    /**
     * Gets the lead section and initial metadata of a given title.
     *
     * @param title the page title with prefix if necessary
     */
    @Headers({
            "x-analytics: pageview=1",
            ACCEPT_HEADER_MOBILE_SECTIONS
    })
    @GET("page/mobile-sections-lead/{title}")
    @NonNull Call<RbPageLead> lead(@Nullable @Header("Cache-Control") String cacheControl,
                                   @Header(SaveHeader.FIELD) Boolean save,
                                   @NonNull @Path("title") String title);

    /**
     * Gets the remaining sections of a given title.
     *
     * @param title the page title to be used including prefix
     */
    @Headers(ACCEPT_HEADER_MOBILE_SECTIONS)
    @GET("page/mobile-sections-remaining/{title}")
    @NonNull Call<RbPageRemaining> sections(@Nullable @Header("Cache-Control") String cacheControl,
                                            @Header(SaveHeader.FIELD) Boolean save,
                                            @NonNull @Path("title") String title);

    // todo: this Content Service-only endpoint is under page/ but that implementation detail should
    //       probably not be reflected here. Move to WordDefinitionClient
    /**
     * Gets selected Wiktionary content for a given title derived from user-selected text
     *
     * @param title the Wiktionary page title derived from user-selected Wikipedia article text
     */
    @Headers(ACCEPT_HEADER_DEFINITION)
    @GET("page/definition/{title}")
    @NonNull Call<Map<String, RbDefinition.Usage[]>> define(@NonNull @Path("title") String title);
}
