package org.wikipedia.nearby;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Response;

class NearbyClient {
    public interface Callback {
        void success(@NonNull Call<MwQueryResponse> call, @NonNull List<NearbyPage> pages);
        void failure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught);
    }

    private static final int MAX_RADIUS = 10_000;

    public Call<MwQueryResponse> request(@NonNull WikiSite wiki, double latitude, double longitude,
                                         double radius, @NonNull Callback cb) {
        return request(wiki, ServiceFactory.get(wiki), latitude, longitude, radius, cb);
    }

    @VisibleForTesting Call<MwQueryResponse> request(@NonNull final WikiSite wiki, @NonNull Service service,
                                                     double latitude, double longitude, double radius,
                                                     @NonNull final Callback cb) {
        String latLng = String.format(Locale.ROOT, "%f|%f", latitude, longitude);
        radius = Math.min(MAX_RADIUS, radius);
        Call<MwQueryResponse> call = service.nearbySearch(latLng, radius);
        call.enqueue(new retrofit2.Callback<MwQueryResponse>() {
            @Override
            public void onResponse(@NonNull Call<MwQueryResponse> call, @NonNull Response<MwQueryResponse> response) {
                // The API results here are unusual in that, if there are no valid results, the
                // response won't even have a "query" key.  Nor will we receive an error.
                // Accordingly, let's assume that we just got an empty result set unless the
                // API explicitly tells us we have an error.
                if (response.body() != null && response.body().success()) {
                    // noinspection ConstantConditions
                    cb.success(call, response.body().query().nearbyPages(wiki));
                } else if (response.body() != null && response.body().hasError()) {
                    // noinspection ConstantConditions
                    cb.failure(call, new MwException(response.body().getError()));
                } else {
                    cb.success(call, Collections.emptyList());
                }
            }

            @Override
            public void onFailure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable t) {
                cb.failure(call, t);
            }
        });
        return call;
    }
}
