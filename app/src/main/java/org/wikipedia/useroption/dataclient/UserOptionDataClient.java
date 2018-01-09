package org.wikipedia.useroption.dataclient;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.WikipediaApp;
import org.wikipedia.csrf.CsrfTokenClient;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwPostResponse;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.retrofit.RetrofitFactory;
import org.wikipedia.useroption.UserOption;
import org.wikipedia.util.log.L;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public class UserOptionDataClient {
    @NonNull private final WikiSite wiki;
    @NonNull private final Service service;

    public interface UserInfoCallback {
        void success(@NonNull UserInfo userInfo);
    }

    public interface UserOptionPostCallback {
        void success();
        void failure(Throwable t);
    }

    public UserOptionDataClient(@NonNull WikiSite wiki) {
        this.wiki = wiki;
        service = RetrofitFactory.newInstance(wiki).create(Service.class);
    }

    public void get(@NonNull final UserInfoCallback callback) {
        // Get a CSRF token, even though we won't use it, to ensure that the user is properly
        // logged in. Otherwise, we might receive user-options for an anonymous IP "user".
        new CsrfTokenClient(wiki, app().getWikiSite()).request(new CsrfTokenClient.DefaultCallback() {
            @Override
            public void success(@NonNull String token) {
                service.get().enqueue(new Callback<MwQueryResponse>() {
                    @Override
                    public void onResponse(Call<MwQueryResponse> call, Response<MwQueryResponse> response) {
                        if (response.body() != null && response.body().success()) {
                            //noinspection ConstantConditions
                            callback.success(response.body().query().userInfo());
                        }
                    }

                    @Override
                    public void onFailure(Call<MwQueryResponse> call, Throwable t) {
                        L.e(t);
                    }
                });
            }
        });
    }

    public void post(@NonNull final UserOption option, @Nullable final UserOptionPostCallback callback) {
        new CsrfTokenClient(wiki, app().getWikiSite()).request(new CsrfTokenClient.DefaultCallback() {
            @Override
            public void success(@NonNull String token) {
                service.post(token, option.key(), option.val()).enqueue(new Callback<PostResponse>() {
                    @Override
                    public void onResponse(Call<PostResponse> call, Response<PostResponse> response) {
                        if (response.body() != null && !response.body().success(response.body().result())) {
                            L.e("Bad response for wiki " + wiki.authority() + " = " + response.body().result());
                        } else if (callback != null) {
                            callback.success();
                        }
                    }

                    @Override
                    public void onFailure(Call<PostResponse> call, Throwable caught) {
                        L.e(caught);
                        if (callback != null) {
                            callback.failure(caught);
                        }
                    }
                });
            }
        });
    }

    public void delete(@NonNull final String key, @Nullable final UserOptionPostCallback callback) {
        new CsrfTokenClient(wiki, app().getWikiSite()).request(new CsrfTokenClient.DefaultCallback() {
            @Override
            public void success(@NonNull String token) {
                service.delete(token, key).enqueue(new Callback<PostResponse>() {
                    @Override
                    public void onResponse(Call<PostResponse> call, Response<PostResponse> response) {
                        if (response.body() != null && !response.body().success(response.body().result())) {
                            L.e("Bad response for wiki " + wiki.authority() + " = " + response.body().result());
                        } else if (callback != null) {
                            callback.success();
                        }
                    }

                    @Override
                    public void onFailure(Call<PostResponse> call, Throwable caught) {
                        L.e(caught);
                        if (callback != null) {
                            callback.failure(caught);
                        }
                    }
                });
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
