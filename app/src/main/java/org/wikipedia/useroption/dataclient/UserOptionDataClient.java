package org.wikipedia.useroption.dataclient;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.WikipediaApp;
import org.wikipedia.csrf.CsrfTokenClient;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwPostResponse;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.useroption.UserOption;
import org.wikipedia.util.log.L;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserOptionDataClient {
    @NonNull private final WikiSite wiki;

    public interface UserInfoCallback {
        void success(@NonNull UserInfo userInfo);
    }

    public interface UserOptionPostCallback {
        void success();
        void failure(Throwable t);
    }

    public UserOptionDataClient(@NonNull WikiSite wiki) {
        this.wiki = wiki;
    }

    public void get(@NonNull final UserInfoCallback callback) {
        // Get a CSRF token, even though we won't use it, to ensure that the user is properly
        // logged in. Otherwise, we might receive user-options for an anonymous IP "user".
        new CsrfTokenClient(wiki, app().getWikiSite()).request(new CsrfTokenClient.DefaultCallback() {
            @Override
            public void success(@NonNull String token) {
                ServiceFactory.get(wiki).getUserOptions().enqueue(new Callback<MwQueryResponse>() {
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
                ServiceFactory.get(wiki).postUserOption(token, option.key(), option.val()).enqueue(new Callback<MwPostResponse>() {
                    @Override
                    public void onResponse(Call<MwPostResponse> call, Response<MwPostResponse> response) {
                        if (response.body() != null && !response.body().success(response.body().getOptions())) {
                            L.e("Bad response for wiki " + wiki.authority() + " = " + response.body().getOptions());
                        } else if (callback != null) {
                            callback.success();
                        }
                    }

                    @Override
                    public void onFailure(Call<MwPostResponse> call, Throwable caught) {
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
                ServiceFactory.get(wiki).deleteUserOption(token, key).enqueue(new Callback<MwPostResponse>() {
                    @Override
                    public void onResponse(Call<MwPostResponse> call, Response<MwPostResponse> response) {
                        if (response.body() != null && !response.body().success(response.body().getOptions())) {
                            L.e("Bad response for wiki " + wiki.authority() + " = " + response.body().getOptions());
                        } else if (callback != null) {
                            callback.success();
                        }
                    }

                    @Override
                    public void onFailure(Call<MwPostResponse> call, Throwable caught) {
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
}
