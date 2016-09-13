package org.wikipedia.login;

import org.wikipedia.Constants;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.server.mwapi.MwServiceError;
import org.wikipedia.util.log.L;

import com.google.gson.annotations.SerializedName;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

/**
 * Responsible for making login related requests to the server.
 */
public class LoginClient {
    public static final String CLIENT_LOGIN = "clientlogin";
    public static final String JSON = "json";

    @NonNull private final MwCachedService<Service> cachedService
            = new MwCachedService<>(Service.class);

    @Nullable private Call<MwQueryResponse<LoginToken>> tokenCall;
    @Nullable private Call<LoginResponse> loginCall;

    public interface LoginCallback {
        void success(@NonNull LoginResult result);
        void error(@NonNull Throwable caught);
    }

    public void request(@NonNull final Site site, @NonNull final String userName,
                        @NonNull final String password, @NonNull final LoginCallback cb) {
        cancel();

        // HACK: T124384
        WikipediaApp.getInstance().getEditTokenStorage().clearAllTokens();

        tokenCall = cachedService.service(site).requestLoginToken();
        tokenCall.enqueue(new Callback<MwQueryResponse<LoginToken>>() {
            @Override
            public void onResponse(Call<MwQueryResponse<LoginToken>> call,
                                   Response<MwQueryResponse<LoginToken>> response) {
                if (response.isSuccessful()) {
                    final MwQueryResponse<LoginToken> body = response.body();
                    if (body.query() != null &&  body.query().getLoginToken() != null) {
                        login(site, userName, password, body.query().getLoginToken(), cb);
                    } else if (body.getError() != null) {
                        L.v(body.getError().toString());
                        cb.error(new IOException("Failed to retrieve login token. "
                                + body.getError().toString()));
                    } else {
                        cb.error(new IOException("Unexpected error trying to retrieve login token. "
                                + body.toString()));
                    }
                } else {
                    cb.error(new IOException(response.message()));
                }
            }

            @Override
            public void onFailure(Call<MwQueryResponse<LoginToken>> call, Throwable caught) {
                cb.error(caught);
            }
        });
    }

    private void login(@NonNull Site site, @NonNull String userName, @NonNull final String password,
                       @NonNull String loginToken, final LoginCallback cb) {
        loginCall = cachedService.service(site).logIn(CLIENT_LOGIN, JSON, userName, password,
                loginToken, Constants.WIKIPEDIA_URL);
        loginCall.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful()) {
                    LoginResponse loginResponse = response.body();
                    LoginResult loginResult = loginResponse.toLoginResult(password);
                    if (loginResult != null) {
                        if (loginResult.pass()) {
                            User.setUser(loginResult.getUser());
                            cb.success(loginResult);
                        } else {
                            cb.error(new LoginFailedException(loginResult.getMessage()));
                        }
                    } else {
                        cb.error(new IOException("Login failed. Unexpected response."));
                    }
                } else {
                    // very unlikely to happen because MW API responds with 200 for failures, too.
                    cb.error(new IOException("Login failed. Unexpected response."));
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                cb.error(t);
            }
        });
    }

    public void cancel() {
        cancelTokenRequest();
        cancelLogin();
    }

    private void cancelTokenRequest() {
        if (tokenCall == null) {
            return;
        }
        tokenCall.cancel();
        tokenCall = null;
    }

    private void cancelLogin() {
        if (loginCall == null) {
            return;
        }
        loginCall.cancel();
        loginCall = null;
    }

    private interface Service {

        /** Request a login token to be used later to log in. */
        @NonNull
        @POST("w/api.php?format=json&formatversion=2&action=query"
                + "&meta=tokens&type=login")
        Call<MwQueryResponse<LoginToken>> requestLoginToken();

        /** Actually log in. Has to be x-www-form-urlencoded */
        @NonNull
        @FormUrlEncoded
        @POST("w/api.php")
        Call<LoginResponse> logIn(@Field("action") String action, @Field("format") String format,
                                  @Field("username") String user, @Field("password") String pass,
                                  @Field("logintoken") String token,
                                  @Field("loginreturnurl") String url);
    }

    private static final class LoginToken {
        @SerializedName("tokens") private Tokens tokens;

        @Nullable public String getLoginToken() {
            return tokens == null ? null : tokens.loginToken;
        }

        private class Tokens {
            @SerializedName("logintoken") @Nullable
            private String loginToken;
        }
    }

    private static final class LoginResponse {
        @SerializedName("error") @Nullable
        private MwServiceError error;

        @SerializedName("clientlogin") @Nullable
        private ClientLogin clientLogin;

        @Nullable
        public MwServiceError getError() {
            return error;
        }

        public LoginResult toLoginResult(String password) {
            return clientLogin != null ? clientLogin.toLoginResult(password) : null;
        }

        private static class ClientLogin {
            @SerializedName("status") private String status;
            @SerializedName("message") @Nullable private String message;
            @SerializedName("username") @Nullable private String userName;

            public LoginResult toLoginResult(String password) {
                User user = null;
                String userMessage = null;
                if ("PASS".equals(status)) {
                    user = new User(userName, password, 0);
                } else if ("FAIL".equals(status)) {
                    userMessage = message;
                }
                return new LoginResult(status, user, userMessage);
            }
        }
    }

    public class LoginFailedException extends Throwable {
        public LoginFailedException(String message) {
            super(message);
        }
    }
}
