package org.wikipedia.language;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.WikiCachedService;
import org.wikipedia.page.PageTitle;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Query;

class LangLinksClient {
    public interface Callback {
        void success(@NonNull Call<MwQueryResponse> call, @NonNull List<PageTitle> links);
        void failure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught);
    }

    @NonNull private final WikiCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    Call<MwQueryResponse> request(@NonNull WikiSite wiki, @NonNull PageTitle title,
                                             @NonNull Callback cb) {
        return request(cachedService.service(wiki), title, cb);
    }

    @VisibleForTesting Call<MwQueryResponse> request(@NonNull Service service,
                                                                @NonNull PageTitle title,
                                                                @NonNull final Callback cb) {
        Call<MwQueryResponse> call = service.langLinks(title.getPrefixedText());
        call.enqueue(new retrofit2.Callback<MwQueryResponse>() {
            @Override
            public void onResponse(Call<MwQueryResponse> call,
                                   Response<MwQueryResponse> response) {
                if (response.body().success()) {
                    // noinspection ConstantConditions
                    cb.success(call, response.body().query().langLinks());
                } else if (response.body().hasError()) {
                    // noinspection ConstantConditions
                    cb.failure(call, new MwException(response.body().getError()));
                } else {
                    cb.failure(call, new IOException("An unknown error occurred."));
                }
            }

            @Override
            public void onFailure(Call<MwQueryResponse> call, Throwable t) {
                cb.failure(call, t);
            }
        });
        return call;
    }

    @VisibleForTesting interface Service {
        @GET("w/api.php?action=query&format=json&formatversion=2&prop=langlinks&lllimit=500&converttitles=")
        Call<MwQueryResponse> langLinks(@NonNull @Query("titles") String title);
    }
}
