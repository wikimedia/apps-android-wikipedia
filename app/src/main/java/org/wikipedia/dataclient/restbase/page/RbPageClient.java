package org.wikipedia.dataclient.restbase.page;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.page.PageClient;
import org.wikipedia.dataclient.page.PageLead;
import org.wikipedia.dataclient.page.PageRemaining;
import org.wikipedia.dataclient.page.PageSummary;

import io.reactivex.Observable;
import okhttp3.CacheControl;
import retrofit2.Call;

// todo: consolidate with MwPageClient or just use the Services directly!
/**
 * Retrofit web service client for RESTBase Nodejs API.
 */
public class RbPageClient implements PageClient {

    // todo: RbPageSummary should specify an @Required annotation that throws a JsonParseException
    //       when the body is null rather than requiring all clients to check for a null body. There
    //       may be some abandoned demo patches that already have this functionality. It should be
    //       part of the Gson augmentation package and eventually cut into a separate lib. Repeat
    //       everywhere a Response.body() == null check occurs that throws
    @SuppressWarnings("unchecked")
    @NonNull @Override public Observable<? extends PageSummary> summary(@NonNull Service service, @NonNull String title, @Nullable String referrerUrl) {
        return service.getSummary(referrerUrl, title);
    }

    @SuppressWarnings("unchecked")
    @NonNull @Override public Call<? extends PageLead> lead(@NonNull Service service,
                                                            @Nullable CacheControl cacheControl,
                                                            @Nullable String saveOfflineHeader,
                                                            @Nullable String referrerUrl,
                                                            @NonNull String title,
                                                            int leadThumbnailWidth) {
        return service.getLeadSection(cacheControl == null ? null : cacheControl.toString(),
                saveOfflineHeader, referrerUrl, title);
    }

    @SuppressWarnings("unchecked")
    @NonNull @Override public Call<? extends PageRemaining> sections(@NonNull Service service,
                                                                     @Nullable CacheControl cacheControl,
                                                                     @Nullable String saveOfflineHeader,
                                                                     @NonNull String title) {
        return service.getRemainingSections(cacheControl == null ? null : cacheControl.toString(),
                saveOfflineHeader, title);
    }
}
