package org.wikipedia.editing;


import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.RetrofitException;
import org.wikipedia.page.PageTitle;
import org.wikipedia.server.restbase.RbPageEndpointsCache;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class WikitextClient {
    @NonNull private final MwCachedService<Service> cachedService = new MwCachedService<>(Service.class);
    @NonNull private final Retrofit retrofit = RbPageEndpointsCache.INSTANCE.getRetrofit();

    public Call<Wikitext> request(@NonNull final WikiSite wiki, @NonNull final PageTitle title,
                                  final int sectionID, @NonNull final Callback cb) {
        Service service = cachedService.service(wiki);
        return request(service, title, sectionID, cb);
    }

    @VisibleForTesting Call<Wikitext> request(@NonNull Service service, @NonNull final PageTitle title,
                           final int sectionID, @NonNull final Callback cb) {
        Call<Wikitext> call = service.wikitext(title.getPrefixedText(), sectionID);
        call.enqueue(new retrofit2.Callback<Wikitext>() {
            @Override
            public void onResponse(Call<Wikitext> call, Response<Wikitext> response) {
                if (response.isSuccessful()) {
                    cb.success(call, response.body().wikitext());
                } else {
                    cb.failure(call, RetrofitException.httpError(response, retrofit));
                }
            }

            @Override
            public void onFailure(Call<Wikitext> call, Throwable t) {
                cb.failure(call, t);
            }
        });
        return call;
    }

    public interface Callback {
        void success(@NonNull Call<Wikitext> call, @NonNull String wikitext);
        void failure(@NonNull Call<Wikitext> call, @NonNull Throwable caught);
    }

    private interface Service {
        @GET("w/api.php?action=query&format=json&prop=revisions&rvprop=content&rvlimit=1")
        Call<Wikitext> wikitext(@NonNull @Query("titles") String title,
                                @Query("rvsection") int section);
    }
}
