package org.wikipedia.pageimages;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import org.wikipedia.Constants;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.page.PageTitle;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class PageImagesClient {
    @NonNull private MwCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    public interface Callback {
        void success(@NonNull Call<MwQueryResponse> call,
                     @NonNull Map<PageTitle, PageImage> results);
        void failure(@NonNull Call<MwQueryResponse> call,
                     @NonNull Throwable caught);
    }

    public Call<MwQueryResponse> request(@NonNull WikiSite wiki,
                                                                @NonNull List<PageTitle> titles,
                                                                @NonNull Callback cb) {
        return request(wiki, cachedService.service(wiki), titles, cb);
    }

    @VisibleForTesting
    Call<MwQueryResponse> request(@NonNull final WikiSite wiki, @NonNull Service service,
                                  @NonNull final List<PageTitle> titles, @NonNull final Callback cb) {
        Call<MwQueryResponse> call = service.request(TextUtils.join("|", titles));
        call.enqueue(new retrofit2.Callback<MwQueryResponse>() {
            @Override public void onResponse(@NonNull Call<MwQueryResponse> call,
                                             @NonNull Response<MwQueryResponse> response) {
                Map<PageTitle, PageImage> pageImagesMap = new ArrayMap<>();
                // error cases
                if (response.body().success()) {
                    // nominal case
                    Map<String, PageTitle> titlesMap = new ArrayMap<>();
                    for (PageTitle title : titles) {
                        titlesMap.put(title.getPrefixedText(), title);
                    }
                    Map<String, String> thumbnailSourcesMap = new ArrayMap<>();

                    // noinspection ConstantConditions
                    for (MwQueryPage page : response.body().query().pages()) {
                        thumbnailSourcesMap.put(new PageTitle(null, page.title(),
                                wiki).getPrefixedText(), page.thumbUrl());
                        if (!TextUtils.isEmpty(page.convertedFrom())) {
                            thumbnailSourcesMap.put(new PageTitle(null, page.convertedFrom(),
                                    wiki).getPrefixedText(), page.thumbUrl());
                        }
                        if (!TextUtils.isEmpty(page.redirectFrom())) {
                            thumbnailSourcesMap.put(new PageTitle(null, page.redirectFrom(),
                                    wiki).getPrefixedText(), page.thumbUrl());
                        }
                    }

                    for (String key : titlesMap.keySet()) {
                        if (thumbnailSourcesMap.containsKey(key)) {
                            PageTitle title = titlesMap.get(key);
                            pageImagesMap.put(title, new PageImage(title, thumbnailSourcesMap.get(key)));
                        }
                    }
                    cb.success(call, pageImagesMap);
                } else if (response.body().hasError()) {
                    // noinspection ConstantConditions
                    cb.failure(call, new MwException(response.body().getError()));
                } else {
                    cb.failure(call, new IOException("An unknown error occurred."));
                }
            }

            @Override public void onFailure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable t) {
                cb.failure(call, t);
            }
        });
        return call;
    }

    @VisibleForTesting interface Service {
        @GET("w/api.php?action=query&format=json&formatversion=2&prop=pageimages&piprop=thumbnail"
                + "&converttitles=&pilicense=any&pithumbsize=" + Constants.PREFERRED_THUMB_SIZE)
        Call<MwQueryResponse> request(@NonNull @Query("titles") String titles);
    }
}
