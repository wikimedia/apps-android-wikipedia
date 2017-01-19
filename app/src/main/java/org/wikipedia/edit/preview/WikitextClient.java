package org.wikipedia.edit.preview;


import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.google.gson.JsonParseException;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwApiErrorException;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.RetrofitException;
import org.wikipedia.page.PageTitle;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class WikitextClient {
    @NonNull private final MwCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    public Call<MwQueryResponse<Wikitext>> request(@NonNull final WikiSite wiki, @NonNull final PageTitle title,
                                  final int sectionID, @NonNull final Callback cb) {
        Service service = cachedService.service(wiki);
        return request(service, title, sectionID, cb);
    }

    @VisibleForTesting Call<MwQueryResponse<Wikitext>> request(@NonNull Service service, @NonNull final PageTitle title,
                                                              final int sectionID, @NonNull final Callback cb) {
        Call<MwQueryResponse<Wikitext>> call = service.request(title.getPrefixedText(), sectionID);
        call.enqueue(new retrofit2.Callback<MwQueryResponse<Wikitext>>() {
            @Override
            public void onResponse(Call<MwQueryResponse<Wikitext>> call, Response<MwQueryResponse<Wikitext>> response) {
                if (response.isSuccessful()) {
                    if (response.body().hasError()) {
                        cb.failure(call, new MwApiErrorException(response.body().getError()));
                        return;
                    } else if (response.body().query().wikitext() == null) {
                        Throwable t = new JsonParseException("Error parsing wikitext from query response");
                        cb.failure(call, t);
                        return;
                    }
                    cb.success(call, response.body().query().wikitext());
                } else {
                    cb.failure(call, RetrofitException.httpError(response, cachedService.retrofit()));
                }
            }

            @Override
            public void onFailure(Call<MwQueryResponse<Wikitext>> call, Throwable t) {
                cb.failure(call, t);
            }
        });
        return call;
    }

    public interface Callback {
        void success(@NonNull Call<MwQueryResponse<Wikitext>> call, @NonNull String wikitext);
        void failure(@NonNull Call<MwQueryResponse<Wikitext>> call, @NonNull Throwable caught);
    }

    private interface Service {
        @GET("w/api.php?action=query&format=json&prop=revisions&rvprop=content&rvlimit=1")
        Call<MwQueryResponse<Wikitext>> request(@NonNull @Query("titles") String title, @Query("rvsection") int section);
    }
}
