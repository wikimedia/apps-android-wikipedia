package org.wikipedia.edit.preview;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

class EditPreviewClient {
    Call<EditPreview> request(@NonNull WikiSite wiki, @NonNull PageTitle title,
                              @NonNull String wikitext, @NonNull Callback cb) {
        return request(ServiceFactory.get(wiki), title, wikitext, cb);
    }

    @VisibleForTesting Call<EditPreview> request(@NonNull Service service, @NonNull PageTitle title,
                                                 @NonNull String wikitext,
                                                 @NonNull final Callback cb) {
        Call<EditPreview> call = service.postEditPreview(title.getPrefixedText(), wikitext);
        call.enqueue(new retrofit2.Callback<EditPreview>() {
            @Override
            public void onResponse(Call<EditPreview> call, Response<EditPreview> response) {
                if (response.body().hasPreviewResult()) {
                    cb.success(call, response.body().result());
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
}
