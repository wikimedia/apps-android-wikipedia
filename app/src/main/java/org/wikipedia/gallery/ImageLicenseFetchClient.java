package org.wikipedia.gallery;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.page.PageTitle;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

public class ImageLicenseFetchClient {
    public interface Callback {
        void success(@NonNull Call<MwQueryResponse> call, @NonNull ImageLicense result);
        void failure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught);
    }

    public Call<MwQueryResponse> request(@NonNull WikiSite wiki,
                                                      @NonNull PageTitle title,
                                                      @NonNull Callback cb) {
        return request(ServiceFactory.get(wiki), title, cb);
    }

    @VisibleForTesting Call<MwQueryResponse> request(@NonNull Service service,
                                                                  @NonNull final PageTitle title,
                                                                  @NonNull final Callback cb) {
        Call<MwQueryResponse> call = service.getImageExtMetadata(title.toString());

        call.enqueue(new retrofit2.Callback<MwQueryResponse>() {
            @Override public void onResponse(Call<MwQueryResponse> call, Response<MwQueryResponse> response) {
                if (response.body().success()) {
                    // noinspection ConstantConditions
                    MwQueryPage page = response.body().query().pages().get(0);
                    cb.success(call, page.imageInfo() != null && page.imageInfo().getMetadata() != null
                            ? new ImageLicense(page.imageInfo().getMetadata())
                            : new ImageLicense());
                } else if (response.body().hasError()) {
                    // noinspection ConstantConditions
                    cb.failure(call, new MwException(response.body().getError()));
                } else {
                    cb.failure(call, new IOException("An unknown error occurred."));
                }
            }

            @Override
            public void onFailure(Call<MwQueryResponse> call, Throwable t) {
                cb.failure(call, t);
            }
        });

        return call;
    }
}
