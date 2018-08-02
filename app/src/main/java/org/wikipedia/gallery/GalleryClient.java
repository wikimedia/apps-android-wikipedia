package org.wikipedia.gallery;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.retrofit.RbCachedService;
import org.wikipedia.dataclient.retrofit.WikiCachedService;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class GalleryClient {
    @NonNull private WikiCachedService<Service> cachedService = new RbCachedService<>(Service.class);

    public interface Callback {
        void success(@NonNull Call<Gallery> call, @NonNull List<GalleryItem> results);
        void failure(@NonNull Call<Gallery> call, @NonNull Throwable caught);
    }

    public Call<Gallery> request(@NonNull String title, @NonNull WikiSite wiki, @NonNull Callback cb, @NonNull String... types) {
        return request(cachedService.service(wiki), title, cb, types);
    }

    @VisibleForTesting
    Call<Gallery> request(@NonNull Service service, @NonNull String title, @NonNull Callback cb, @NonNull String...types) {

        Call<Gallery> call = service.fetch(title);

        call.enqueue(new retrofit2.Callback<Gallery>() {
            @Override public void onResponse(@NonNull Call<Gallery> call,
                                             @NonNull Response<Gallery> response) {
                if (response.body() != null && response.body().getAllItems() != null) {
                    // noinspection ConstantConditions
                    cb.success(call, types.length == 0 ? response.body().getAllItems() : response.body().getItems(types));
                } else {
                    cb.failure(call, new IOException("An unknown error occurred."));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Gallery> call, @NonNull Throwable t) {
                cb.failure(call, t);
            }
        });
        return call;
    }

    @VisibleForTesting
    interface Service {
        @NonNull
        @GET("page/media/{title}")
        Call<Gallery> fetch(@Path("title") String title);
    }
}
