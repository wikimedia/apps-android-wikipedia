package org.wikipedia.dataclient.page;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import okhttp3.CacheControl;
import retrofit2.Call;

/**
 * Generic interface for Page content service.
 * Usually we would use direct Retrofit Callbacks here but since we have two ways of
 * getting to the data (MW API and RESTBase) we add this layer of indirection -- until we drop one.
 */
public interface PageClient {
    enum CacheOption {
        /** Request transient app caching; if the response is already cached in permanent storage,
            it will be refreshed regardless */
        CACHE,

        /** Request persistent app caching; if headers permit the response to be cached, it will be
            saved to the permanent cache */
        SAVE;

        public boolean save() {
            return this == SAVE;
        }
    }

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
    @NonNull <T extends PageLead> Call<T> lead(@Nullable CacheControl cacheControl,
                                               @NonNull CacheOption cacheOption,
                                               @NonNull String title,
                                               int leadThumbnailWidth,
                                               boolean noImages);

    /**
     * Gets the remaining sections of a given title.
     *
     * @param title the page title to be used including prefix
     * @param noImages add the noimages flag to the request if true
     */
    @NonNull <T extends PageRemaining> Call<T> sections(@Nullable CacheControl cacheControl,
                                                        @NonNull CacheOption cacheOption,
                                                        @NonNull String title,
                                                        boolean noImages);
}
