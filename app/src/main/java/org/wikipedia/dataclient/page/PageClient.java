package org.wikipedia.dataclient.page;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;

import io.reactivex.Observable;
import okhttp3.CacheControl;
import okhttp3.Request;
import retrofit2.Response;

public class PageClient {

    // todo: RbPageSummary should specify an @Required annotation that throws a JsonParseException
    //       when the body is null rather than requiring all clients to check for a null body. There
    //       may be some abandoned demo patches that already have this functionality. It should be
    //       part of the Gson augmentation package and eventually cut into a separate lib. Repeat
    //       everywhere a Response.body() == null check occurs that throws
    @NonNull public Observable<Response<PageSummary>> summary(@NonNull WikiSite wiki, @NonNull String title, @Nullable String referrerUrl) {
        return ServiceFactory.getRest(wiki).getSummary(referrerUrl, title);
    }

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

    @NonNull public Request sectionsUrl(@NonNull WikiSite wiki,
                                        @Nullable CacheControl cacheControl,
                                        @Nullable String saveOfflineHeader,
                                        @NonNull String title) {
        return ServiceFactory.getRest(wiki).getRemainingSectionsUrl(cacheControl == null ? null : cacheControl.toString(),
                saveOfflineHeader, title).request();
    }
}
