package org.wikipedia.dataclient.page;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.dataclient.WikiSite;

import io.reactivex.Observable;
import okhttp3.CacheControl;
import okhttp3.Request;
import retrofit2.Response;

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
    @NonNull <T extends PageSummary> Observable<T> summary(@NonNull WikiSite wiki,
                                                           @NonNull String title,
                                                           @Nullable String referrerUrl);

    /**
     * Gets the lead section and initial metadata of a given title.
     *
     * @param title the page title with prefix if necessary
     * @param leadThumbnailWidth one of the bucket widths for the lead image
     */
    @NonNull <T extends PageLead> Observable<Response<T>> lead(@NonNull WikiSite wiki,
                                                               @Nullable CacheControl cacheControl,
                                                               @Nullable String saveOfflineHeader,
                                                               @Nullable String referrerUrl,
                                                               @NonNull String title,
                                                               int leadThumbnailWidth);

    /**
     * Gets the remaining sections of a given title.
     *
     * @param title the page title to be used including prefix
     */
    @NonNull <T extends PageRemaining> Observable<Response<T>> sections(@NonNull WikiSite wiki,
                                                                        @Nullable CacheControl cacheControl,
                                                                        @Nullable String saveOfflineHeader,
                                                                        @NonNull String title);

    /**
     * Gets the remaining sections request url of a given title.
     *
     * @param title the page title to be used including prefix
     */
    @NonNull Request sectionsUrl(@NonNull WikiSite wiki,
                               @Nullable CacheControl cacheControl,
                               @Nullable String saveOfflineHeader,
                               @NonNull String title);
}
