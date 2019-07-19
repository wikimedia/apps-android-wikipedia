package org.wikipedia.edit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.wikipedia.captcha.CaptchaResult;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

class EditClient {

    @Nullable private Call<Edit> editCall;

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
        editCall = service.postEditSubmit(title.getPrefixedText(), section, summary, loggedIn ? "user" : null,
                text, baseTimeStamp, token, captchaId, captchaWord);
        editCall.enqueue(new retrofit2.Callback<Edit>() {
            @Override
            public void onResponse(@NonNull Call<Edit> call, @NonNull Response<Edit> response) {
                if (call.isCanceled()) {
                    return;
                }
                if (response.body().hasEditResult()) {
                    handleEditResult(response.body().edit(), call, cb);
                } else {
                    cb.failure(call, new IOException("An unknown error occurred."));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Edit> call, @NonNull Throwable t) {
                if (call.isCanceled()) {
                    return;
                }
                cb.failure(call, t);
            }
        });
        return editCall;
    }

    public void cancel() {
        if (editCall == null) {
            return;
        }
        editCall.cancel();
        editCall = null;
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
