package org.wikipedia.dataclient.mwapi.page;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.page.PageClient;
import org.wikipedia.dataclient.page.PageLead;
import org.wikipedia.dataclient.page.PageRemaining;
import org.wikipedia.dataclient.page.PageSummary;

import okhttp3.CacheControl;
import retrofit2.Call;

/**
 * Retrofit web service client for MediaWiki PHP API.
 */
public class MwPageClient implements PageClient {
    @NonNull private final MwPageService service;

    public MwPageClient(@NonNull MwPageService service) {
        this.service = service;
    }

    @SuppressWarnings("unchecked")
    @NonNull @Override public Call<? extends PageSummary> summary(@NonNull String title) {
        return service.summary(title);
    }

    @SuppressWarnings("unchecked")
    @NonNull @Override public Call<? extends PageLead> lead(@Nullable CacheControl cacheControl,
                                                            @NonNull CacheOption cacheOption,
                                                            @NonNull String title,
                                                            int leadImageWidth) {
        return service.lead(cacheControl == null ? null : cacheControl.toString(),
                optional(cacheOption.save()), title, leadImageWidth);
    }

    @SuppressWarnings("unchecked")
    @NonNull @Override public Call<? extends PageRemaining> sections(@Nullable CacheControl cacheControl,
                                                                     @NonNull CacheOption cacheOption,
                                                                     @NonNull String title) {
        return service.sections(cacheControl == null ? null : cacheControl.toString(),
                optional(cacheOption.save()), title);
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
}
