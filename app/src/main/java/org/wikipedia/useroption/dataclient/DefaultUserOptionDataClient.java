package org.wikipedia.useroption.dataclient;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.WikipediaApp;
import org.wikipedia.csrf.CsrfToken;
import org.wikipedia.csrf.CsrfTokenClient;
import org.wikipedia.dataclient.ServiceError;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwPostResponse;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.retrofit.RetrofitFactory;
import org.wikipedia.useroption.UserOption;
import org.wikipedia.util.log.L;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public class DefaultUserOptionDataClient implements UserOptionDataClient {
    @NonNull private final WikiSite wiki;
    @NonNull private final Service service;

    public DefaultUserOptionDataClient(@NonNull WikiSite wiki) {
        this.wiki = wiki;
        service = RetrofitFactory.newInstance(wiki).create(Service.class);
    }

    @NonNull
    @Override
    public UserInfo get() throws IOException {
        Response<MwQueryResponse<QueryUserInfo>> rsp = service.get().execute();
        if (rsp.isSuccessful() && rsp.body().success()) {
            //noinspection ConstantConditions
            return rsp.body().query().userInfo();
        }
        ServiceError err = rsp.body() == null || rsp.body().getError() == null
                ? null
                : rsp.body().getError();
        throw new IOException(err == null ? rsp.message() : err.getDetails());
    }

    @Override
    public void post(@NonNull UserOption option) throws IOException {
        Response<PostResponse> rsp = service.post(getToken(), option.key(), option.val()).execute();
        if (rsp.isSuccessful()) {
            rsp.body().check(wiki);
            return;
        }

        String msg = rsp.body() == null || rsp.body().info() == null
                ? rsp.message()
                : rsp.body().info();
        throw new IOException(msg);
    }

    @Override
    public void delete(@NonNull String key) throws IOException {
        service.delete(getToken(), key).execute().body().check(wiki);
    }

    @NonNull private String getToken() throws IOException {
        if (app().getCsrfTokenStorage().token(wiki) == null) {
            requestToken();
        }

        String token = app().getCsrfTokenStorage().token(wiki);
        if (token == null) {
            throw new IOException("No token for " + wiki.authority());
        }
        return token;
    }

    private void requestToken() {
        new CsrfTokenClient().request(wiki, new CsrfTokenClient.Callback() {
            @Override
            public void success(@NonNull Call<MwQueryResponse<CsrfToken>> call, @NonNull String token) {
                app().getCsrfTokenStorage().token(wiki, token);
            }

            @Override
            public void failure(@NonNull Call<MwQueryResponse<CsrfToken>> call, @NonNull Throwable caught) {
                // Don't worry about it; will be retried next time.
                L.w(caught);
            }
        });
    }

    private static WikipediaApp app() {
        return WikipediaApp.getInstance();
    }

    // todo: rename service
    private interface Service {
        String ACTION = "w/api.php?format=json&formatversion=2&action=";

        @GET(ACTION + "query&meta=userinfo&uiprop=options")
        @NonNull Call<MwQueryResponse<QueryUserInfo>> get();

        @FormUrlEncoded
        @POST(ACTION + "options")
        @NonNull Call<PostResponse> post(@Field("token") @NonNull String token,
                                         @Query("optionname") @NonNull String key,
                                         @Query("optionvalue") @Nullable String value);

        @FormUrlEncoded
        @POST(ACTION + "options")
        @NonNull Call<PostResponse> delete(@Field("token") @NonNull String token,
                                           @Query("change") @NonNull String key);
    }

    private static class PostResponse extends MwPostResponse {
        @SuppressWarnings("unused") private String options;

        public String result() {
            return options;
        }

        public void check(@NonNull WikiSite wiki) throws IOException {
            if (!success(options)) {
                if (badToken()) {
                    app().getCsrfTokenStorage().token(wiki, null);
                }

                throw new IOException("Bad response for wiki " + wiki.host() + " = " + result());
            }
        }
    }

    private static class QueryUserInfo {
        @SuppressWarnings("unused") @SerializedName("userinfo") private UserInfo userInfo;

        UserInfo userInfo() {
            return userInfo;
        }
    }
}
