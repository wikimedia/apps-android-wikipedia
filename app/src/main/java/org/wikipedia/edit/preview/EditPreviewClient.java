package org.wikipedia.edit.preview;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.WikiCachedService;
import org.wikipedia.page.PageTitle;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

class EditPreviewClient {
    @NonNull private final WikiCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    Call<EditPreview> request(@NonNull WikiSite wiki, @NonNull PageTitle title,
                              @NonNull String wikitext, @NonNull Callback cb) {
        Service service = cachedService.service(wiki);
        return request(service, title, wikitext, cb);
    }

    @VisibleForTesting Call<EditPreview> request(@NonNull Service service, @NonNull PageTitle title,
                                                 @NonNull String wikitext,
                                                 @NonNull final Callback cb) {
        Call<EditPreview> call = service.previewEdit(title.getPrefixedText(), wikitext);
        call.enqueue(new retrofit2.Callback<EditPreview>() {
            @Override
            public void onResponse(Call<EditPreview> call, Response<EditPreview> response) {
                if (response.body().success() && response.body().hasPreviewResult()) {
                    cb.success(call, response.body().result());
                } else if (response.body().hasError()) {
                    cb.failure(call, new MwException(response.body().getError()));
                } else {
                    cb.failure(call, new IOException("An unknown error occurred."));
                }
            }

            @Override
            public void onFailure(Call<EditPreview> call, Throwable t) {
                cb.failure(call, t);
            }
        });
        return call;
    }

    public interface Callback {
        void success(@NonNull Call<EditPreview> call, @NonNull String preview);
        void failure(@NonNull Call<EditPreview> call, @NonNull Throwable caught);
    }

    @VisibleForTesting interface Service {
        @FormUrlEncoded
        @POST("w/api.php?action=parse&format=json&formatversion=2&prop=text&sectionpreview=&pst="
                + "&mobileformat=")
        Call<EditPreview> previewEdit(@NonNull @Field("title") String title,
                                      @NonNull @Field("text") String text);
    }
}
