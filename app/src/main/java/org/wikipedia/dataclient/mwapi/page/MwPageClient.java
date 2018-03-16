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
                                                            @Nullable String saveOfflineHeader,
                                                            @NonNull String title,
                                                            int leadImageWidth) {
        return service.lead(cacheControl == null ? null : cacheControl.toString(),
                saveOfflineHeader, title, leadImageWidth);
    }

    @SuppressWarnings("unchecked")
    @NonNull @Override public Call<? extends PageRemaining> sections(@Nullable CacheControl cacheControl,
                                                                     @Nullable String saveOfflineHeader,
                                                                     @NonNull String title) {
        return service.sections(cacheControl == null ? null : cacheControl.toString(),
                saveOfflineHeader, title);
    }
}
