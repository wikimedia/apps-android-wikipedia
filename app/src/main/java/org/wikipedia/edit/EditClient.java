package org.wikipedia.edit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.captcha.CaptchaResult;
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
import retrofit2.http.Headers;
import retrofit2.http.POST;

class EditClient {
    public interface Callback {
        void success(@NonNull Call<Edit> call, @NonNull EditResult result);
        void failure(@NonNull Call<Edit> call, @NonNull Throwable caught);
    }

    @NonNull private final WikiCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    @SuppressWarnings("checkstyle:parameternumber")
    public Call<Edit> request(@NonNull WikiSite wiki, @NonNull PageTitle title, int section,
                              @NonNull String text, @NonNull String token, @NonNull String summary,
                              boolean loggedIn, @Nullable String captchaId, @Nullable String captchaWord,
                              @NonNull Callback cb) {
        return request(cachedService.service(wiki), title, section, text, token, summary, loggedIn,
                captchaId, captchaWord, cb);
    }

    @VisibleForTesting @SuppressWarnings("checkstyle:parameternumber")
    Call<Edit> request(@NonNull Service service, @NonNull PageTitle title, int section,
                       @NonNull String text, @NonNull String token, @NonNull String summary,
                       boolean loggedIn, @Nullable String captchaId, @Nullable String captchaWord,
                       @NonNull final Callback cb) {
        Call<Edit> call = service.edit(title.getPrefixedText(), section, text, token, summary,
                loggedIn ? "user" : null, captchaId, captchaWord);
        call.enqueue(new retrofit2.Callback<Edit>() {
            @Override
            public void onResponse(Call<Edit> call, Response<Edit> response) {
                if (response.body().hasEditResult()) {
                    handleEditResult(response.body().edit(), call, cb);
                } else if (response.body().hasError()) {
                    RuntimeException e = response.body().badLoginState()
                            ? new UserNotLoggedInException()
                            : new MwException(response.body().getError());
                    cb.failure(call, e);
                } else {
                    cb.failure(call, new IOException("An unknown error occurred."));
                }
            }

            @Override
            public void onFailure(Call<Edit> call, Throwable t) {
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

    @VisibleForTesting interface Service {
        @FormUrlEncoded
        @Headers("Cache-Control: no-cache")
        @POST("w/api.php?action=edit&format=json&formatversion=2&nocreate=")
        @SuppressWarnings("checkstyle:parameternumber")
        Call<Edit> edit(@NonNull @Field("title") String title,
                        @Field("section") int section,
                        @NonNull @Field("text") String text,
                        @NonNull @Field("token") String token,
                        @NonNull @Field("summary") String summary,
                        @Nullable @Field("assert") String user,
                        @Nullable @Field("captchaid") String captchaId,
                        @Nullable @Field("captchaword") String captchaWord);
    }
}
