package org.wikipedia.dataclient.restbase.page;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.JsonParseException;

import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.page.PageClient;
import org.wikipedia.dataclient.page.PageLead;
import org.wikipedia.dataclient.page.PageRemaining;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.dataclient.restbase.RbDefinition;

import java.util.Map;

import okhttp3.CacheControl;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// todo: consolidate with MwPageClient or just use the Services directly!
/**
 * Retrofit web service client for RESTBase Nodejs API.
 */
public class RbPageClient implements PageClient {
    public interface DefinitionCallback {
        void success(@NonNull RbDefinition definition);
        void failure(@NonNull Throwable throwable);
    }

    // todo: RbPageSummary should specify an @Required annotation that throws a JsonParseException
    //       when the body is null rather than requiring all clients to check for a null body. There
    //       may be some abandoned demo patches that already have this functionality. It should be
    //       part of the Gson augmentation package and eventually cut into a separate lib. Repeat
    //       everywhere a Response.body() == null check occurs that throws
    @SuppressWarnings("unchecked")
    @NonNull @Override public Call<? extends PageSummary> summary(@NonNull Service service, @NonNull String title, @Nullable String referrerUrl) {
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

    /* Not defined in the PageClient interface since the Wiktionary definition endpoint exists only
     * in the mobile content service, and does not concern the wholesale retrieval of the contents
     * of a wiki page.
     */
    public void define(@NonNull Service service, String title, final DefinitionCallback cb) {
        Call<Map<String, RbDefinition.Usage[]>> call = service.getDefinition(title);
        call.enqueue(new Callback<Map<String, RbDefinition.Usage[]>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, RbDefinition.Usage[]>> call,
                                   @NonNull Response<Map<String, RbDefinition.Usage[]>> response) {
                if (response.body() == null) {
                    cb.failure(new JsonParseException("Response missing required fields"));
                    return;
                }
                cb.success(new RbDefinition(response.body()));
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, RbDefinition.Usage[]>> call, @NonNull Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                cb.failure(throwable);
            }
        });
    }
}
