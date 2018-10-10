package org.wikipedia.edit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.captcha.CaptchaResult;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

class EditClient {
    public interface Callback {
        void success(@NonNull Call<Edit> call, @NonNull EditResult result);
        void failure(@NonNull Call<Edit> call, @NonNull Throwable caught);
    }

    @SuppressWarnings("checkstyle:parameternumber")
    public Call<Edit> request(@NonNull WikiSite wiki, @NonNull PageTitle title, int section,
                              @NonNull String text, @NonNull String token, @NonNull String summary,
                              @Nullable String baseTimeStamp, boolean loggedIn, @Nullable String captchaId,
                              @Nullable String captchaWord, @NonNull Callback cb) {
        return request(ServiceFactory.get(wiki), title, section, text, token, summary,
                baseTimeStamp, loggedIn, captchaId, captchaWord, cb);
    }

    @VisibleForTesting @SuppressWarnings("checkstyle:parameternumber")
    Call<Edit> request(@NonNull Service service, @NonNull PageTitle title, int section,
                       @NonNull String text, @NonNull String token, @NonNull String summary,
                       @Nullable String baseTimeStamp, boolean loggedIn, @Nullable String captchaId,
                       @Nullable String captchaWord, @NonNull final Callback cb) {
        Call<Edit> call = service.postEditSubmit(title.getPrefixedText(), section, summary, loggedIn ? "user" : null,
                text, baseTimeStamp, token, captchaId, captchaWord);
        call.enqueue(new retrofit2.Callback<Edit>() {
            @Override
            public void onResponse(@NonNull Call<Edit> call, @NonNull Response<Edit> response) {
                if (response.body().hasEditResult()) {
                    handleEditResult(response.body().edit(), call, cb);
                } else {
                    cb.failure(call, new IOException("An unknown error occurred."));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Edit> call, @NonNull Throwable t) {
                cb.failure(call, t);
            }
        });
        return call;
    }

    private void handleEditResult(@NonNull Edit.Result result, @NonNull Call<Edit> call,
                                  @NonNull Callback cb) {
        if (result.editSucceeded()) {
            cb.success(call, new EditSuccessResult(result.newRevId()));
        } else if (result.hasEditErrorCode()) {
            cb.success(call, new EditAbuseFilterResult(result.code(), result.info(), result.warning()));
        } else if (result.hasSpamBlacklistResponse()) {
            cb.success(call, new EditSpamBlacklistResult(result.spamblacklist()));
        } else if (result.hasCaptchaResponse()) {
            cb.success(call, new CaptchaResult(result.captchaId()));
        } else {
            cb.failure(call, new IOException("Received unrecognized edit response"));
        }
    }
}
