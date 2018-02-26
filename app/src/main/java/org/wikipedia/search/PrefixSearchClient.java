package org.wikipedia.search;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.Constants;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.WikiCachedService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class PrefixSearchClient {
    public interface Callback {
        void success(@NonNull Call<PrefixSearchResponse> call, @NonNull SearchResults results);
        void failure(@NonNull Call<PrefixSearchResponse> call, @NonNull Throwable caught);
    }

    @NonNull private final WikiCachedService<Service> cachedService = new MwCachedService<>(Service.class);
    @Nullable private Call<PrefixSearchResponse> call;

    // TODO: Seems arbitrary, consider updating
    private static final int MAX_RESULTS = 20;

    public Call<PrefixSearchResponse> request(@NonNull WikiSite wiki, @NonNull String title,
                                              @NonNull Callback cb) {
        return request(cachedService.service(wiki), wiki, title, cb);
    }

    @VisibleForTesting Call<PrefixSearchResponse> request(@NonNull Service service,
                                                          @NonNull final WikiSite wiki,
                                                          @NonNull String title,
                                                          @NonNull final Callback cb) {
        call = service.request(title, title);
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
            public void onFailure(@NonNull Call<PrefixSearchResponse> call,
                                  @NonNull Throwable caught) {
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

    @VisibleForTesting interface Service {
        String QUERY_PREFIX = "w/api.php?action=query&format=json&formatversion=2&redirects="
                + "&converttitles=&prop=pageterms|pageimages&wbptterms=description&piprop=thumbnail"
                + "&pilicense=any&generator=prefixsearch&gpsnamespace=0&list=search&srnamespace=0"
                + "&srwhat=text&srinfo=suggestion&srprop=&sroffset=0&srlimit=1&gpslimit="
                + MAX_RESULTS + "&pithumbsize=" + Constants.PREFERRED_THUMB_SIZE;

        @GET(QUERY_PREFIX)
        @NonNull Call<PrefixSearchResponse> request(@Query("gpssearch") String title,
                                                    @Query("srsearch") String repeat);
    }
}
