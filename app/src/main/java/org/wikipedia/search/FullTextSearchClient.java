package org.wikipedia.search;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import retrofit2.Call;
import retrofit2.Response;

public class FullTextSearchClient {
    public interface Callback {
        void success(@NonNull Call<MwQueryResponse> call, @NonNull SearchResults results);
        void failure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught);
    }

    @Nullable private Call<MwQueryResponse> call;

    public Call<MwQueryResponse> request(@NonNull WikiSite wiki, @NonNull String searchTerm,
                                         @Nullable String cont, @Nullable String gsrOffset,
                                         int limit, @NonNull Callback cb) {
        return request(ServiceFactory.get(wiki), wiki, searchTerm, cont, gsrOffset, limit, cb);
    }

    @VisibleForTesting Call<MwQueryResponse> request(@NonNull Service service,
                                                     @NonNull final WikiSite wiki,
                                                     @NonNull String searchTerm,
                                                     @Nullable String cont,
                                                     @Nullable String gsrOffset,
                                                     int limit, @NonNull final Callback cb) {
        call = service.fullTextSearch(searchTerm, limit, cont, gsrOffset);
        call.enqueue(new retrofit2.Callback<MwQueryResponse>() {
            @Override
            public void onResponse(@NonNull Call<MwQueryResponse> call,
                                   @NonNull Response<MwQueryResponse> response) {
                if (response.body().success()) {
                    // noinspection ConstantConditions
                    cb.success(call, new SearchResults(response.body().query().pages(), wiki,
                            response.body().continuation(), null));
                } else if (response.body().hasError()) {
                    // noinspection ConstantConditions
                    cb.failure(call, new MwException(response.body().getError()));
                } else {
                    // A 'morelike' search query with no results will just return an API warning:
                    //
                    // {
                    //   "batchcomplete": true,
                    //   "warnings": {
                    //      "search": {
                    //        "warnings": "No valid titles provided to 'morelike'."
                    //      }
                    //   }
                    // }
                    //
                    // Just return an empty SearchResults() in this case.
                    cb.success(call, new SearchResults());
                }
            }

            @Override
            public void onFailure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught) {
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
