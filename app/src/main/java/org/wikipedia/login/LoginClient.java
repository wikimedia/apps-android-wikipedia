package org.wikipedia.login;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Constants;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.mwapi.MwServiceError;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.WikiCachedService;
import org.wikipedia.util.log.L;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * Responsible for making login related requests to the server.
 */
public class LoginClient {
    @NonNull private final WikiCachedService<Service> cachedService
            = new MwCachedService<>(Service.class);

    @Nullable private Call<MwQueryResponse<LoginToken>> tokenCall;
    @Nullable private Call<LoginResponse> loginCall;

    public interface LoginCallback {
        void success(@NonNull LoginResult result);
        void twoFactorPrompt(@NonNull Throwable caught, @Nullable String token);
        void error(@NonNull Throwable caught);
    }

    public void request(@NonNull final WikiSite wiki, @NonNull final String userName,
                        @NonNull final String password, @NonNull final LoginCallback cb) {
        cancel();

        // HACK: T124384
        WikipediaApp.getInstance().getCsrfTokenStorage().clearAllTokens();

        tokenCall = cachedService.service(wiki).requestLoginToken();
        tokenCall.enqueue(new Callback<MwQueryResponse<LoginToken>>() {
            @Override
            public void onResponse(Call<MwQueryResponse<LoginToken>> call,
                                   Response<MwQueryResponse<LoginToken>> response) {
                if (response.isSuccessful()) {
                    MwQueryResponse<LoginToken> body = response.body();
                    LoginToken query = body.query();
                    if (query != null &&  query.getLoginToken() != null) {
                        login(wiki, userName, password, null, query.getLoginToken(), cb);
                    } else if (body.getError() != null) {
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

    void login(@NonNull final WikiSite wiki, @NonNull final String userName,
                       @NonNull final String password, @Nullable final String twoFactorCode,
                       @Nullable final String loginToken, @NonNull final LoginCallback cb) {
        loginCall = StringUtils.defaultIfEmpty(twoFactorCode, "").isEmpty()
                ? cachedService.service(wiki).logIn(userName, password, loginToken, Constants.WIKIPEDIA_URL)
                : cachedService.service(wiki).logIn(userName, password, twoFactorCode, loginToken, true);
        loginCall.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful()) {
                    LoginResponse loginResponse = response.body();
                    LoginResult loginResult = loginResponse.toLoginResult(password);
                    if (loginResult != null) {
                        if (loginResult.pass() && loginResult.getUser() != null) {
                            // The server could do some transformations on user names, e.g. on some
                            // wikis is uppercases the first letter.
                            String actualUserName = loginResult.getUser().getUsername();
                            getExtendedInfo(wiki, actualUserName, loginResult, cb);
                        } else if ("UI".equals(loginResult.getStatus())) {
                            //TODO: Don't just assume this is a 2FA UI result
                            cb.twoFactorPrompt(new LoginFailedException(loginResult.getMessage()), loginToken);
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

    private void getExtendedInfo(@NonNull final WikiSite wiki, @NonNull String userName,
                                 @NonNull final LoginResult loginResult, @NonNull final LoginCallback cb) {
        UserExtendedInfoClient infoClient = new UserExtendedInfoClient();
        infoClient.request(wiki, userName, new UserExtendedInfoClient.Callback() {
            @Override
            public void success(@NonNull Call<MwQueryResponse<UserExtendedInfoClient.QueryResult>> call,
                                int id, @NonNull Set<String> groups) {
                final User user = loginResult.getUser();
                User.setUser(new User(user, id, wiki.languageCode(), groups));
                cb.success(loginResult);
                L.v("Found user ID " + id + " for " + wiki.languageCode());
            }

            @Override
            public void failure(@NonNull Call<MwQueryResponse<UserExtendedInfoClient.QueryResult>> call,
                              @NonNull Throwable caught) {
                L.e("Login suceeded but getting group information failed. " + caught);
                cb.error(caught);
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
        @POST("w/api.php?action=clientlogin&format=json&rememberMe=true")
        Call<LoginResponse> logIn(@Field("username") String user, @Field("password") String pass,
                                  @Field("logintoken") String token, @Field("loginreturnurl") String url);

        /** Actually log in. Has to be x-www-form-urlencoded */
        @NonNull
        @FormUrlEncoded
        @POST("w/api.php?action=clientlogin&format=json&rememberMe=true")
        Call<LoginResponse> logIn(@Field("username") String user, @Field("password") String pass,
                                  @Field("OATHToken") String twoFactorCode, @Field("logintoken") String token,
                                  @Field("logincontinue") boolean loginContinue);
    }

    private static final class LoginToken {
        @SerializedName("tokens") private Tokens tokens;

        @Nullable String getLoginToken() {
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

        LoginResult toLoginResult(String password) {
            return clientLogin != null ? clientLogin.toLoginResult(password) : null;
        }

        private static class ClientLogin {
            @SerializedName("status") private String status;
            @Nullable private List<Request> requests;
            @SerializedName("message") @Nullable private String message;
            @SerializedName("username") @Nullable private String userName;

            LoginResult toLoginResult(String password) {
                User user = null;
                String userMessage = null;
                if ("PASS".equals(status)) {
                    user = new User(userName, password);
                } else if ("FAIL".equals(status)) {
                    userMessage = message;
                } else if ("UI".equals(status)) {
                    if (requests != null) {
                        for (Request req : requests) {
                            if ("TOTPAuthenticationRequest".equals(req.id())) {
                                return new LoginOAuthResult(status, message);
                            }
                        }
                    }
                    userMessage = message;
                } else {
                    //TODO: String resource -- Looks like needed for others in this class too
                    userMessage = "An unknown error occurred.";
                }
                return new LoginResult(status, user, userMessage);
            }
        }

        private static class Request {
            @SuppressWarnings("unused") @Nullable private String id;
            //@SuppressWarnings("unused") @Nullable private JsonObject metadata;
            @SuppressWarnings("unused") @Nullable private String required;
            @SuppressWarnings("unused") @Nullable private String provider;
            @SuppressWarnings("unused") @Nullable private String account;
            @SuppressWarnings("unused") @Nullable private Map<String, RequestField> fields;

            @Nullable String id() {
                return id;
            }
        }

        private static class RequestField {
            @SuppressWarnings("unused") @Nullable private String type;
            @SuppressWarnings("unused") @Nullable private String label;
            @SuppressWarnings("unused") @Nullable private String help;
        }
    }

    public static class LoginFailedException extends Throwable {
        public LoginFailedException(String message) {
            super(message);
        }
    }
}
