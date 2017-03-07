package org.wikipedia.dataclient.restbase.page;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.JsonParseException;

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

    // todo: why not hold a reference to a WikiCachedService and require clients to pass a WikiSite
    //       with each request?
    @NonNull private final RbPageService service;

    public RbPageClient(@NonNull RbPageService service) {
        this.service = service;
    }

    // todo: RbPageSummary should specify an @Required annotation that throws a JsonParseException
    //       when the body is null rather than requiring all clients to check for a null body. There
    //       may be some abandoned demo patches that already have this functionality. It should be
    //       part of the Gson augmentation package and eventually cut into a separate lib. Repeat
    //       everywhere a Response.body() == null check occurs that throws
    @SuppressWarnings("unchecked")
    @NonNull @Override public Call<? extends PageSummary> summary(@NonNull String title) {
        return service.summary(title);
    }

    @SuppressWarnings("unchecked")
    @NonNull @Override public Call<? extends PageLead> lead(@Nullable CacheControl cacheControl,
                                                            @NonNull CacheOption cacheOption,
                                                            @NonNull String title,
                                                            int leadThumbnailWidth,
                                                            boolean noImages) {
        return service.lead(cacheControl == null ? null : cacheControl.toString(),
                optional(cacheOption.save()), title, optional(noImages));
    }

    @SuppressWarnings("unchecked")
    @NonNull @Override public Call<? extends PageRemaining> sections(@Nullable CacheControl cacheControl,
                                                                     @NonNull CacheOption cacheOption,
                                                                     @NonNull String title,
                                                                     boolean noImages) {
        return service.sections(cacheControl == null ? null : cacheControl.toString(),
                optional(cacheOption.save()), title, optional(noImages));
    }

    /* Not defined in the PageClient interface since the Wiktionary definition endpoint exists only
     * in the mobile content service, and does not concern the wholesale retrieval of the contents
     * of a wiki page.
     */
    public void define(String title, final DefinitionCallback cb) {
        Call<Map<String, RbDefinition.Usage[]>> call = service.define(title);
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
            public void onFailure(Call<Map<String, RbDefinition.Usage[]>> call, Throwable throwable) {
                cb.failure(throwable);
            }
        });
    }

    // todo: consolidate MwPageClient and RbPageClient.optional() in util
    /**
     * Optional boolean Retrofit parameter.
     * We don't want to send the query parameter at all when it's false since the presence of the
     * parameter alone is enough to trigger the truthy behavior.
     */
    private Boolean optional(boolean param) {
        if (param) {
            return true;
        }
        return null;
    }
}
