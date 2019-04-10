package org.wikipedia.dataclient.mwapi.page;

import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageClient;
import org.wikipedia.dataclient.page.PageSummary;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.Observable;
import okhttp3.CacheControl;
import okhttp3.Request;
import retrofit2.Response;

/**
 * Retrofit web service client for MediaWiki PHP API.
 */
public class MwPageClient implements PageClient {

    @SuppressWarnings("unchecked")
    @NonNull @Override public Observable<? extends PageSummary> summary(@NonNull WikiSite wiki, @NonNull String title, @Nullable String referrerUrl) {
        return ServiceFactory.get(wiki).getSummary(referrerUrl, title, wiki.languageCode());
    }

    @SuppressWarnings("unchecked")
    @NonNull @Override public Observable<Response<MwMobileViewPageLead>> lead(@NonNull WikiSite wiki,
                                                                              @Nullable CacheControl cacheControl,
                                                                              @Nullable String saveOfflineHeader,
                                                                              @Nullable String referrerUrl,
                                                                              @NonNull String title,
                                                                              int leadImageWidth) {
        return ServiceFactory.get(wiki).getLeadSection(cacheControl == null ? null : cacheControl.toString(),
                saveOfflineHeader, referrerUrl, title, leadImageWidth, wiki.languageCode());
    }

    @SuppressWarnings("unchecked")
    @NonNull @Override public Observable<Response<MwMobileViewPageRemaining>> sections(@NonNull WikiSite wiki,
                                                                                       @Nullable CacheControl cacheControl,
                                                                                       @Nullable String saveOfflineHeader,
                                                                                       @NonNull String title) {
        return ServiceFactory.get(wiki).getRemainingSections(cacheControl == null ? null : cacheControl.toString(),
                saveOfflineHeader, title, wiki.languageCode());
    }

    @SuppressWarnings("unchecked")
    @NonNull @Override public Request sectionsUrl(@NonNull WikiSite wiki,
                                                  @Nullable CacheControl cacheControl,
                                                  @Nullable String saveOfflineHeader,
                                                  @NonNull String title) {
        return ServiceFactory.get(wiki).getRemainingSectionsUrl(cacheControl == null ? null : cacheControl.toString(),
                saveOfflineHeader, title, wiki.languageCode()).request();
    }
}
