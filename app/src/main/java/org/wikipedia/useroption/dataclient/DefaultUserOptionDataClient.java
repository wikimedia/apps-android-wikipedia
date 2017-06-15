package org.wikipedia.useroption.dataclient;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.WikipediaApp;
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
import retrofit2.Callback;
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
        Response<MwQueryResponse> rsp = service.get().execute();
        if (rsp.body().success()) {
            //noinspection ConstantConditions
            return rsp.body().query().userInfo();
        }
        ServiceError err = rsp.body() == null || rsp.body().getError() == null
                ? null
                : rsp.body().getError();
        throw new IOException(err == null ? rsp.message() : err.getDetails());
    }

    @Override
    public void post(@NonNull final UserOption option) throws IOException {
        new CsrfTokenClient(wiki, app().getWikiSite()).request(new TokenCallback() {
            @Override
            public void success(@NonNull String token) {
                service.post(token, option.key(), option.val()).enqueue(new Callback<PostResponse>() {
                    @Override
                    public void onResponse(Call<PostResponse> call, Response<PostResponse> response) {
                        if (response.body() != null && !response.body().success(response.body().result())) {
                            L.e("Bad response for wiki " + wiki.host() + " = " + response.body().result());
                        }
                        notifyThis();
                    }

                    @Override
                    public void onFailure(Call<PostResponse> call, Throwable caught) {
                        L.e(caught);
                        notifyThis();
                    }
                });
            }
        });
        waitForThis();
    }

    @Override
    public void delete(@NonNull final String key) throws IOException {
        new CsrfTokenClient(wiki, app().getWikiSite()).request(new TokenCallback() {
            @Override
            public void success(@NonNull String token) {
                service.delete(token, key).enqueue(new Callback<PostResponse>() {
                    @Override
                    public void onResponse(Call<PostResponse> call, Response<PostResponse> response) {
                        if (response.body() != null && !response.body().success(response.body().result())) {
                            L.e("Bad response for wiki " + wiki.host() + " = " + response.body().result());
                        }
                        notifyThis();
                    }

                    @Override
                    public void onFailure(Call<PostResponse> call, Throwable caught) {
                        L.e(caught);
                        notifyThis();
                    }
                });
            }
        });
        waitForThis();
    }

    private synchronized void waitForThis() {
        try {
            wait();
        } catch (InterruptedException e) {
            L.d(e);
        }
    }

    private synchronized void notifyThis() {
        notify();
    }

    private static WikipediaApp app() {
        return WikipediaApp.getInstance();
    }

    private class TokenCallback implements CsrfTokenClient.Callback {
        @Override
        public void success(@NonNull String token) {
        }

        @Override
        public void failure(@NonNull Throwable caught) {
            L.e(caught);
        }

        @Override
        public void twoFactorPrompt() {
            // TODO: warn the user that they need to re-login with 2FA.
        }
    }

    // todo: rename service
    private interface Service {
        String ACTION = "w/api.php?format=json&formatversion=2&action=";

        @GET(ACTION + "query&meta=userinfo&uiprop=options")
        @NonNull Call<MwQueryResponse> get();

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
    }
}
