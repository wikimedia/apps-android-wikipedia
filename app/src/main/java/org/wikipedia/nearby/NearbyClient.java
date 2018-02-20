package org.wikipedia.nearby;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.Constants;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.WikiCachedService;

import java.util.ArrayList;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Query;

class NearbyClient {
    public interface Callback {
        void success(@NonNull Call<MwQueryResponse> call, @NonNull NearbyResult result);
        void failure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught);
    }

    private static final int MAX_RADIUS = 10_000;

    @NonNull private final WikiCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    public Call<MwQueryResponse> request(@NonNull WikiSite wiki, double latitude, double longitude,
                                         double radius, @NonNull Callback cb) {
        return request(wiki, cachedService.service(wiki), latitude, longitude, radius, cb);
    }

    @VisibleForTesting Call<MwQueryResponse> request(@NonNull final WikiSite wiki, @NonNull Service service,
                                                     double latitude, double longitude, double radius,
                                                     @NonNull final Callback cb) {
        String latLng = String.format(Locale.ROOT, "%f|%f", latitude, longitude);
        radius = Math.min(MAX_RADIUS, radius);
        Call<MwQueryResponse> call = service.request(latLng, radius, Constants.PREFERRED_THUMB_SIZE);
        call.enqueue(new retrofit2.Callback<MwQueryResponse>() {
            @Override
            public void onResponse(Call<MwQueryResponse> call, Response<MwQueryResponse> response) {
                // The API results here are unusual in that, if there are no valid results, the
                // response won't even have a "query" key.  Nor will we receive an error.
                // Accordingly, let's assume that we just got an empty result set unless the
                // API explicitly tells us we have an error.
                if (response.body().success()) {
                    // noinspection ConstantConditions
                    cb.success(call, new NearbyResult(wiki, response.body().query().nearbyPages()));
                } else if (response.body().hasError()) {
                    // noinspection ConstantConditions
                    cb.failure(call, new MwException(response.body().getError()));
                } else {
                    cb.success(call, new NearbyResult(wiki, new ArrayList<>()));
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
        @GET("w/api.php?action=query&format=json&formatversion=2&prop=coordinates|pageimages|pageterms"
                + "&colimit=50&piprop=thumbnail&pilicense=any&wbptterms=description"
                + "&generator=geosearch&ggslimit=50&continue=")
        Call<MwQueryResponse> request(@NonNull @Query("ggscoord") String coord,
                                      @Query("ggsradius") double radius,
                                      @Query("pithumbsize") int thumbsize);
    }
}
