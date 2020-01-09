package org.wikipedia.dataclient.page;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;

import io.reactivex.Observable;
import okhttp3.CacheControl;
import retrofit2.Response;

public class PageClient {

    @NonNull public Observable<Response<PageLead>> lead(@NonNull WikiSite wiki,
                                                        @Nullable CacheControl cacheControl,
                                                        @Nullable String saveOfflineHeader,
                                                        @Nullable String referrerUrl,
                                                        @NonNull String title,
                                                        int leadThumbnailWidth) {
        return ServiceFactory.getRest(wiki).getLeadSection(cacheControl == null ? null : cacheControl.toString(),
                saveOfflineHeader, referrerUrl, title);
    }

    @NonNull public Observable<Response<PageRemaining>> sections(@NonNull WikiSite wiki,
                                                                 @Nullable CacheControl cacheControl,
                                                                 @Nullable String saveOfflineHeader,
                                                                 @NonNull String title) {
        return ServiceFactory.getRest(wiki).getRemainingSections(cacheControl == null ? null : cacheControl.toString(),
                saveOfflineHeader, title);
    }
}
