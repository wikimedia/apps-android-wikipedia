package org.wikipedia.gallery;


import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.WikiCachedService;
import org.wikipedia.page.PageTitle;

import java.io.IOException;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Query;

class GalleryItemClient {

    public interface Callback {
        void success(@NonNull Call<MwQueryResponse> call, @NonNull GalleryItem result);

        void failure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught);
    }

    private static final String MAX_IMAGE_WIDTH = "1280";

    private final WikiCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    public Call<MwQueryResponse> request(@NonNull WikiSite wiki,
                                         @NonNull PageTitle title,
                                         @NonNull Callback cb,
                                         boolean isVideo) {
        return request(cachedService.service(wiki), title, cb, isVideo);
    }

    @VisibleForTesting
    Call<MwQueryResponse> request(@NonNull final Service service,
                                  @NonNull final PageTitle title,
                                  @NonNull final Callback cb,
                                  final boolean isVideo) {
        Call<MwQueryResponse> call = (isVideo) ? service.requestVideo(title.toString())
                : service.requestImage(title.toString());
        call.enqueue(new retrofit2.Callback<MwQueryResponse>() {
            @Override
            public void onResponse(@NonNull Call<MwQueryResponse> call, @NonNull Response<MwQueryResponse> response) {
                MwQueryResponse mwQueryResponse = response.body();
                if (mwQueryResponse != null && mwQueryResponse.success() && mwQueryResponse.query() != null) {

                    GalleryItem galleryItem = null;
                    if (isVideo) {
                        Map<String, VideoInfo> m = mwQueryResponse.query().videos();
                        if (m.size() != 0) {
                            String key = m.keySet().iterator().next();
                            galleryItem = new GalleryItem(key, m.get(key));
                        }
                    } else {
                        Map<String, ImageInfo> m = mwQueryResponse.query().images();
                        if (m.size() != 0) {
                            String key = m.keySet().iterator().next();
                            galleryItem = new GalleryItem(key, m.get(key));
                        }
                    }
                    if (galleryItem != null) {
                        cb.success(call, galleryItem);
                    } else {
                        cb.failure(call, new IOException("An unknown error occurred."));
                    }
                } else if (mwQueryResponse != null && mwQueryResponse.hasError()) {
                    // noinspection ConstantConditions
                    cb.failure(call, new MwException(response.body().getError()));
                } else {
                    cb.failure(call, new IOException("Null gallery item received."));
                }
            }

            @Override
            public void onFailure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable t) {
                cb.failure(call, t);
            }
        });
        return call;
    }

    @VisibleForTesting
    interface Service {
        @GET("w/api.php?action=query&format=json&formatversion=2&continue=&prop=imageinfo"
                + "&iiprop=url|dimensions|mime|extmetadata&iiurlwidth=" + MAX_IMAGE_WIDTH)
        Call<MwQueryResponse> requestImage(@NonNull @Query("titles") String titles);

        @GET("w/api.php?action=query&format=json&formatversion=2&continue=&prop=videoinfo"
                + "&viprop=url|dimensions|mime|extmetadata|derivatives&viurlwidth=" + MAX_IMAGE_WIDTH)
        Call<MwQueryResponse> requestVideo(@NonNull @Query("titles") String titles);
    }
}
