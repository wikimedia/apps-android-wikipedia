package org.wikipedia.edit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.captcha.CaptchaResult;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.page.PageTitle;

import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

class EditClient {
    @NonNull private final MwCachedService<Service> cachedService
            = new MwCachedService<>(Service.class);

    @SuppressWarnings("checkstyle:parameternumber")
    public Call<Edit> request(@NonNull WikiSite wiki, @NonNull PageTitle title, int section,
                        @NonNull String text, @NonNull String token, @NonNull String summary,
                        boolean loggedIn, @Nullable String captchaId, @Nullable String captchaWord,
                        @NonNull Callback cb) {
        Service service = cachedService.service(wiki);
        return request(service, title, section, text, token, summary, loggedIn, captchaId, captchaWord, cb);
    }

    @SuppressWarnings("checkstyle:parameternumber")
    @VisibleForTesting Call<Edit> request(@NonNull Service service, @NonNull PageTitle title, int section,
                       @NonNull String text, @NonNull String token, @NonNull String summary,
                       boolean loggedIn, @Nullable String captchaId, @Nullable String captchaWord,
                       @NonNull final Callback cb) {
        Call<Edit> call = service.edit(title.getPrefixedText(), section, text,
                token, summary, loggedIn ? "user" : null, captchaId, captchaWord);
        call.enqueue(new retrofit2.Callback<Edit>() {
            @Override
            public void onResponse(Call<Edit> call, Response<Edit> response) {
                if (response.isSuccessful() && response.body().hasEditResult()) {
                    Edit.Result result = response.body().edit();
                    if ("Success".equals(result.status())) {
                        try {
                            // TODO: remove when the server reflects the updated page content
                            // immediately after submitting the edit, instead of a short while after.
                            Thread.sleep(TimeUnit.SECONDS.toMillis(2));
                            cb.success(call, new EditSuccessResult(result.newRevId()));
                        } catch (InterruptedException e) {
                            cb.failure(call, e);
                        }
                    } else if (result.hasErrorCode()) {
                        try {
                            JSONObject json = new JSONObject(GsonMarshaller.marshal(result));
                            cb.success(call, new EditAbuseFilterResult(json));
                        } catch (JSONException e) {
                            cb.failure(call, e);
                        }
                    } else if (result.hasSpamBlacklistResponse()) {
                        cb.success(call, new EditSpamBlacklistResult(result.spamblacklist()));
                    } else if (result.hasCaptchaResponse()) {
                        cb.success(call, new CaptchaResult(result.captchaId()));
                    } else {
                        cb.failure(call, new RuntimeException("Received unrecognized edit response"));
                    }
                } else if (response.body().info() != null) {
                    String info = response.body().info();
                    cb.failure(call, new RuntimeException(info));
                } else {
                    cb.failure(call, new RuntimeException("Received unrecognized edit response"));
                }
            }

            @Override
            public void onFailure(Call<Edit> call, Throwable t) {
                cb.failure(call, t);
            }
        });
        return call;
    }

    @VisibleForTesting interface Callback {
        void success(@NonNull Call<Edit> call, @NonNull EditResult result);
        void failure(@NonNull Call<Edit> call, @NonNull Throwable caught);
    }

    @VisibleForTesting interface Service {
        @FormUrlEncoded
        @POST("w/api.php?action=edit&format=json")
        @SuppressWarnings("checkstyle:parameternumber")
        Call<Edit> edit(@NonNull @Field("title") String title,
                        @Field("section") int section,
                        @NonNull @Field("text") String text,
                        @NonNull @Field("token") String token,
                        @NonNull @Field("summary") String summary,
                        @Nullable @Field("assert") String user,
                        @Nullable @Field("captchaId") String captchaId,
                        @Nullable @Field("captchaWord") String captchaWord);
    }
}
