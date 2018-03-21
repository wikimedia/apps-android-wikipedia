package org.wikipedia.edit.wikitext;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.google.gson.JsonParseException;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.mwapi.MwQueryResult;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.WikiCachedService;
import org.wikipedia.page.PageTitle;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class WikitextClient {
    @NonNull private final WikiCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    public Call<MwQueryResponse> request(@NonNull final WikiSite wiki, @NonNull final PageTitle title,
                                                   final int sectionID, @NonNull final Callback cb) {
        Service service = cachedService.service(wiki);
        return request(service, title, sectionID, cb);
    }

    @VisibleForTesting Call<MwQueryResponse> request(@NonNull Service service, @NonNull final PageTitle title,
                                                              final int sectionID, @NonNull final Callback cb) {
        Call<MwQueryResponse> call = service.request(title.getPrefixedText(), sectionID);
        call.enqueue(new retrofit2.Callback<MwQueryResponse>() {
            @Override
            public void onResponse(@NonNull Call<MwQueryResponse> call, @NonNull Response<MwQueryResponse> response) {
                // noinspection ConstantConditions
                if (response.body() != null && response.body().success()
                        && response.body().query() != null
                        && response.body().query().firstPage() != null
                        && getRevision(response.body().query()) != null) {
                    // noinspection ConstantConditions
                    MwQueryPage.Revision rev = getRevision(response.body().query());
                    cb.success(call, response.body().query().firstPage().title(),
                            rev.content(), rev.timeStamp());
                } else if (response.body() != null && response.body().hasError()) {
                    // noinspection ConstantConditions
                    cb.failure(call, new MwException(response.body().getError()));
                } else {
                    Throwable t = new JsonParseException("Error parsing wikitext from query response");
                    cb.failure(call, t);
                }
            }

            @Override
            public void onFailure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable t) {
                cb.failure(call, t);
            }
        });
        return call;
    }

    @Nullable private MwQueryPage.Revision getRevision(@Nullable MwQueryResult result) {
        if (result != null && result.pages() != null && result.pages().size() > 0) {
            MwQueryPage page = result.pages().get(0);
            if (page.revisions() != null && page.revisions().size() > 0) {
                return page.revisions().get(0);
            }
        }
        return null;
    }

    public interface Callback {
        void success(@NonNull Call<MwQueryResponse> call, @NonNull String normalizedTitle,
                     @NonNull String wikitext, @Nullable String baseTimeStamp);
        void failure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught);
    }

    @VisibleForTesting interface Service {
        @GET("w/api.php?action=query&format=json&formatversion=2&prop=revisions&rvprop=content|timestamp&rvlimit=1&converttitles=")
        Call<MwQueryResponse> request(@NonNull @Query("titles") String title, @Query("rvsection") int section);
    }
}
