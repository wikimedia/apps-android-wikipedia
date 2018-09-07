package org.wikipedia.search;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryPage;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class PrefixSearchClient {
    public interface Callback {
        void success(@NonNull Call<PrefixSearchResponse> call, @NonNull SearchResults results);
        void failure(@NonNull Call<PrefixSearchResponse> call, @NonNull Throwable caught);
    }

    @Nullable private Call<PrefixSearchResponse> call;

    // TODO: Seems arbitrary, consider updating
    private static final int MAX_RESULTS = 20;

    public Call<PrefixSearchResponse> request(@NonNull WikiSite wiki, @NonNull String title,
                                              @NonNull Callback cb) {
        return request(ServiceFactory.get(wiki), wiki, title, cb);
    }

    @VisibleForTesting Call<PrefixSearchResponse> request(@NonNull Service service,
                                                          @NonNull final WikiSite wiki,
                                                          @NonNull String title,
                                                          @NonNull final Callback cb) {
        call = service.prefixSearch(title, MAX_RESULTS, title);
        call.enqueue(new retrofit2.Callback<PrefixSearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<PrefixSearchResponse> call,
                                   @NonNull Response<PrefixSearchResponse> response) {
                if (response.body() != null && response.body().success() && response.body().query().pages() != null) {
                    // noinspection ConstantConditions
                    List<MwQueryPage> pages = response.body().query().pages();
                    // noinspection ConstantConditions
                    cb.success(call, new SearchResults(pages, wiki, response.body().continuation(),
                            response.body().suggestion()));
                } else if (response.body() != null && response.body().hasError()) {
                    // noinspection ConstantConditions
                    cb.failure(call, new MwException(response.body().getError()));
                } else {
                    // A prefix search query with no results will return the following:
                    //
                    // {
                    //   "batchcomplete": true,
                    //   "query": {
                    //      "search": []
                    //   }
                    // }
                    //
                    // Just return an empty SearchResults() in this case.
                    cb.success(call, new SearchResults());
                }
            }

            @Override
            public void onFailure(@NonNull Call<PrefixSearchResponse> call, @NonNull Throwable caught) {
                if (call.isCanceled()) {
                    return;
                }
                cb.failure(call, caught);
            }
        });
        return call;
    }

    void cancel() {
        if (call != null) {
            call.cancel();
            call = null;
        }
    }
}
