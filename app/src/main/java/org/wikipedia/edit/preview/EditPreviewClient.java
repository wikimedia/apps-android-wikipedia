package org.wikipedia.edit.preview;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.RetrofitException;
import org.wikipedia.page.PageTitle;
import org.wikipedia.server.restbase.RbPageEndpointsCache;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

class EditPreviewClient {
    @NonNull private final MwCachedService<Service> cachedService
            = new MwCachedService<>(Service.class);
    @NonNull private final Retrofit retrofit = RbPageEndpointsCache.INSTANCE.getRetrofit();

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
                if (response.isSuccessful()) {
                    EditPreview preview = response.body();
                    if (preview.hasPreviewResult()) {
                        cb.success(call, preview.result());
                    } else if (preview.info() != null) {
                        cb.failure(call, new RuntimeException(preview.info()));
                    } else {
                        cb.failure(call, new RuntimeException("Received unrecognized edit preview response"));
                    }
                } else {
                    cb.failure(call, RetrofitException.httpError(response, retrofit));
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

    private interface Service {
        @FormUrlEncoded
        @POST("w/api.php?action=parse&format=json&sectionpreview=true&pst=true&mobileformat=true"
                + "&prop=text")
        Call<EditPreview> previewEdit(@NonNull @Field("title") String title,
                                      @NonNull @Field("text") String text);
    }
}
