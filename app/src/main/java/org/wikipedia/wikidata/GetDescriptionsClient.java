package org.wikipedia.wikidata;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.page.PageTitle;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class GetDescriptionsClient {
    @NonNull private MwCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    public interface Callback {
        void success(@NonNull Call<MwQueryResponse<QueryResult>> call, @NonNull List<MwQueryPage> results);
        void failure(@NonNull Call<MwQueryResponse<QueryResult>> call, @NonNull Throwable caught);
    }

    public Call<MwQueryResponse<QueryResult>> request(@NonNull WikiSite wiki,
                                                      @NonNull List<PageTitle> titles,
                                                      @NonNull Callback cb) {
        return request(wiki, cachedService.service(wiki), titles, cb);
    }

    @VisibleForTesting Call<MwQueryResponse<QueryResult>> request(final WikiSite wiki, @NonNull Service service,
                                                                  @NonNull final List<PageTitle> titles,
                                                                  @NonNull final Callback cb) {
        Call<MwQueryResponse<QueryResult>> call = service.request(TextUtils.join("|", titles));

        call.enqueue(new retrofit2.Callback<MwQueryResponse<QueryResult>>() {
            @Override public void onResponse(Call<MwQueryResponse<QueryResult>> call,
                                             Response<MwQueryResponse<QueryResult>> response) {
                if (response.body().success()) {
                    cb.success(call, response.body().query().pages());
                } else if (response.body().hasError()) {
                    cb.failure(call, new MwException(response.body().getError()));
                } else {
                    cb.failure(call, new IOException("An unknown error occurred."));
                }
            }

            @Override
            public void onFailure(Call<MwQueryResponse<QueryResult>> call, Throwable t) {
                cb.failure(call, t);
            }
        });

        return call;
    }

    public class QueryResult {
        @SuppressWarnings("unused") @Nullable private List<MwQueryPage> pages;
        @Nullable List<MwQueryPage> pages() {
            return pages;
        }
    }

    @VisibleForTesting interface Service {
        @GET("w/api.php?action=query&format=json&formatversion=2&prop=pageterms&wbptterms=description")
        Call<MwQueryResponse<QueryResult>> request(@NonNull @Query("titles") String titles);

    }
}
