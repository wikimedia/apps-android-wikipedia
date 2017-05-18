package org.wikipedia.readinglist;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import org.wikipedia.Constants;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.RetrofitException;
import org.wikipedia.page.PageTitle;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class ReadingListPageInfoClient {
    @NonNull private MwCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    public interface Callback {
        void success(@NonNull Call<MwQueryResponse<MwQueryResponse.Pages>> call,
                     @NonNull List<MwQueryPage> results);
        void failure(@NonNull Call<MwQueryResponse<MwQueryResponse.Pages>> call,
                     @NonNull Throwable caught);
    }

    public Call<MwQueryResponse<MwQueryResponse.Pages>> request(@NonNull WikiSite wiki,
                                                      @NonNull List<PageTitle> titles,
                                                      @NonNull Callback cb) {
        return request(cachedService.service(wiki), titles, cb);
    }

    @VisibleForTesting
    public Call<MwQueryResponse<MwQueryResponse.Pages>> request(@NonNull Service service,
                                                      @NonNull List<PageTitle> titles,
                                                      @NonNull final Callback cb) {
        Call<MwQueryResponse<MwQueryResponse.Pages>> call = service.request(TextUtils.join("|", titles), titles.size());
        call.enqueue(new retrofit2.Callback<MwQueryResponse<MwQueryResponse.Pages>>() {
            @Override public void onResponse(Call<MwQueryResponse<MwQueryResponse.Pages>> call,
                                             Response<MwQueryResponse<MwQueryResponse.Pages>> response) {
                if (response.isSuccessful()) {
                    if (response.body().success()) {
                        // noinspection ConstantConditions
                        cb.success(call, response.body().query().pages());
                    } else if (response.body().hasError()) {
                        // noinspection ConstantConditions
                        cb.failure(call, new MwException(response.body().getError()));
                    } else {
                        cb.failure(call, new IOException("An unknown error occurred."));
                    }

                } else {
                    cb.failure(call, RetrofitException.httpError(response));
                }
            }

            @Override public void onFailure(Call<MwQueryResponse<MwQueryResponse.Pages>> call, Throwable t) {
                cb.failure(call, t);
            }
        });
        return call;
    }

    @VisibleForTesting interface Service {
        @GET("w/api.php?action=query&format=json&formatversion=2&prop=pageimages|pageterms"
                + "&piprop=thumbnail&pilicense=any&continue=&wbptterms=description&pithumbsize="
                + Constants.PREFERRED_THUMB_SIZE)
        Call<MwQueryResponse<MwQueryResponse.Pages>> request(@NonNull @Query("titles") String titles,
                                                             @Query("pilimit") int piLimit);
    }
}
