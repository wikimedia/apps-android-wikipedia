package org.wikipedia.dataclient.mwapi.page;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageClient;
import org.wikipedia.dataclient.page.PageLead;
import org.wikipedia.dataclient.page.PageRemaining;
import org.wikipedia.dataclient.page.PageSummary;

import io.reactivex.Observable;
import okhttp3.CacheControl;
import retrofit2.Call;

/**
 * Retrofit web service client for MediaWiki PHP API.
 */
public class MwPageClient implements PageClient {
    @NonNull private final WikiSite wiki;

    public MwPageClient(@NonNull WikiSite wiki) {
        this.wiki = wiki;
    }

    @SuppressWarnings("unchecked")
    @NonNull @Override public Observable<? extends PageSummary> summary(@NonNull Service service, @NonNull String title, @Nullable String referrerUrl) {
        return service.getSummary(referrerUrl, title, wiki.languageCode());
    }

    @SuppressWarnings("unchecked")
    @NonNull @Override public Call<? extends PageLead> lead(@NonNull Service service,
                                                            @Nullable CacheControl cacheControl,
                                                            @Nullable String saveOfflineHeader,
                                                            @Nullable String referrerUrl,
                                                            @NonNull String title,
                                                            int leadImageWidth) {
        return service.getLeadSection(cacheControl == null ? null : cacheControl.toString(),
                saveOfflineHeader, referrerUrl, title, leadImageWidth, wiki.languageCode());
    }

    @SuppressWarnings("unchecked")
    @NonNull @Override public Call<? extends PageRemaining> sections(@NonNull Service service,
                                                                     @Nullable CacheControl cacheControl,
                                                                     @Nullable String saveOfflineHeader,
                                                                     @NonNull String title) {
        return service.getRemainingSections(cacheControl == null ? null : cacheControl.toString(),
                saveOfflineHeader, title, wiki.languageCode());
    }
}
