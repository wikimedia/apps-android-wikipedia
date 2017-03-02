package org.wikipedia.language;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.RetrofitException;
import org.wikipedia.dataclient.retrofit.WikiCachedService;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.log.L;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Query;

class LangLinksClient {
    public interface Callback {
        void success(@NonNull Call<MwQueryResponse<LangLinks>> call, @NonNull List<PageTitle> links);
        void failure(@NonNull Call<MwQueryResponse<LangLinks>> call, @NonNull Throwable caught);
    }

    @NonNull private final WikiCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    Call<MwQueryResponse<LangLinks>> request(@NonNull WikiSite wiki, @NonNull PageTitle title,
                                             @NonNull Callback cb) {
        return request(cachedService.service(wiki), title, cb);
    }

    @VisibleForTesting Call<MwQueryResponse<LangLinks>> request(@NonNull Service service,
                                                                @NonNull PageTitle title,
                                                                @NonNull final Callback cb) {
        Call<MwQueryResponse<LangLinks>> call = service.langLinks(title.getPrefixedText());
        call.enqueue(new retrofit2.Callback<MwQueryResponse<LangLinks>>() {
            @Override
            public void onResponse(Call<MwQueryResponse<LangLinks>> call,
                                   Response<MwQueryResponse<LangLinks>> response) {
                if (response.isSuccessful()) {
                    if (response.body().success()) {
                        cb.success(call, response.body().query().langLinks());
                    } else if (response.body().hasError()) {
                        String errorTitle = response.body().getError().getTitle();
                        cb.failure(call, new MwException(response.body().getError()));
                        L.e("API error fetching langlinks: " + errorTitle);
                    } else {
                        cb.failure(call, new IOException("An unknown error occurred."));
                    }
                } else {
                    cb.failure(call, RetrofitException.httpError(response));
                }
            }

            @Override
            public void onFailure(Call<MwQueryResponse<LangLinks>> call, Throwable t) {
                cb.failure(call, t);
            }
        });
        return call;
    }

    @VisibleForTesting interface Service {
        @GET("w/api.php?action=query&format=json&prop=langlinks&lllimit=500&continue=")
        Call<MwQueryResponse<LangLinks>> langLinks(@NonNull @Query("titles") String title);
    }
}
