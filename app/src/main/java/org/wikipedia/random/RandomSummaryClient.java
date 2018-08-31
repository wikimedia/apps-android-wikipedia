package org.wikipedia.random;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.google.gson.JsonParseException;

import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.restbase.page.RbPageSummary;
import org.wikipedia.util.log.L;

import retrofit2.Call;
import retrofit2.Response;

public class RandomSummaryClient {
    public Call<RbPageSummary> request(@NonNull WikiSite wiki, @NonNull Callback cb) {
        return request(ServiceFactory.get(wiki), cb);
    }

    @VisibleForTesting Call<RbPageSummary> request(@NonNull Service service,
                                                   @NonNull final Callback cb) {
        Call<RbPageSummary> call = service.getRandomSummary();
        call.enqueue(new retrofit2.Callback<RbPageSummary>() {
            @Override
            public void onResponse(@NonNull Call<RbPageSummary> call,
                                   @NonNull Response<RbPageSummary> response) {
                if (response.body() == null) {
                    cb.onError(call, new JsonParseException("Response missing required field(s)"));
                    return;
                }
                cb.onSuccess(call, response.body());
            }

            @Override
            public void onFailure(@NonNull Call<RbPageSummary> call, @NonNull Throwable t) {
                L.w("Failed to get random page title/summary", t);
                cb.onError(call, t);
            }
        });
        return call;
    }

    public interface Callback {
        void onSuccess(@NonNull Call<RbPageSummary> call, @NonNull RbPageSummary pageSummary);
        void onError(@NonNull Call<RbPageSummary> call, @NonNull Throwable t);
    }
}
