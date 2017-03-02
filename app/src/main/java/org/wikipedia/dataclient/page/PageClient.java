package org.wikipedia.dataclient.page;

import android.support.annotation.NonNull;

import java.io.IOException;

import retrofit2.Call;

/**
 * Generic interface for Page content service.
 * Usually we would use direct Retrofit Callbacks here but since we have two ways of
 * getting to the data (MW API and RESTBase) we add this layer of indirection -- until we drop one.
 */
public interface PageClient {
    /**
     * Gets a page summary for a given title -- for link previews
     *
     * @param title the page title to be used including prefix
     */
    @NonNull <T extends PageSummary> Call<T> summary(@NonNull String title);

    /**
     * Gets the lead section and initial metadata of a given title.
     *
     * @param title the page title with prefix if necessary
     * @param leadThumbnailWidth one of the bucket widths for the lead image
     * @param noImages add the noimages flag to the request if true
     */
    @NonNull <T extends PageLead> Call<T> lead(@NonNull String title, int leadThumbnailWidth,
                                               boolean noImages);

    /**
     * Gets the remaining sections of a given title.
     *
     * @param title the page title to be used including prefix
     * @param noImages add the noimages flag to the request if true
     */
    @NonNull <T extends PageRemaining> Call<T> sections(@NonNull String title, boolean noImages);

    /**
     * Gets all page content of a given title.  Used in the saved page sync background service.
     * Synchronous call.
     *
     * @param title the page title to be used including prefix
     * @param noImages add the noimages flag to the request if true
     * @throws IOException when the request did not succeed
     */
    PageCombo pageCombo(String title, boolean noImages) throws IOException;
}
