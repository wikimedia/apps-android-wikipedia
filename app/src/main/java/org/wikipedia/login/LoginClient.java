package org.wikipedia.login;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Constants;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
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

    @Nullable private Call<MwQueryResponse> tokenCall;
    @Nullable private Call<LoginResponse> loginCall;

    public interface LoginCallback {
        void success(@NonNull LoginResult result);
        void twoFactorPrompt(@NonNull Throwable caught, @Nullable String token);
        void error(@NonNull Throwable caught);
    }

    public void request(@NonNull final WikiSite wiki, @NonNull final String userName,
                        @NonNull final String password, @NonNull final LoginCallback cb) {
        cancel();

        tokenCall = cachedService.service(wiki).requestLoginToken();
        tokenCall.enqueue(new Callback<MwQueryResponse>() {
            @Override public void onResponse(Call<MwQueryResponse> call,
                                             Response<MwQueryResponse> response) {
                if (response.body().success()) {
                    // noinspection ConstantConditions
                    login(wiki, userName, password, null, response.body().query().loginToken(), cb);
                } else if (response.body().getError() != null) {
                    // noinspection ConstantConditions
                    cb.error(new MwException(response.body().getError()));
                } else {
                    cb.error(new IOException("Unexpected error trying to retrieve login token. "
                            + response.body().toString()));
                }
            }

            @Override
            public void onFailure(Call<MwQueryResponse> call, Throwable caught) {
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
                LoginResponse loginResponse = response.body();
                LoginResult loginResult = loginResponse.toLoginResult(wiki, password);
                if (loginResult != null) {
                    if (loginResult.pass() && !TextUtils.isEmpty(loginResult.getUserName())) {
                        // The server could do some transformations on user names, e.g. on some
                        // wikis is uppercases the first letter.
                        String actualUserName = loginResult.getUserName();
                        getExtendedInfo(wiki, actualUserName, loginResult, cb);
                    } else if ("UI".equals(loginResult.getStatus())) {
                        if (loginResult instanceof LoginOAuthResult) {
                            cb.twoFactorPrompt(new LoginFailedException(loginResult.getMessage()), loginToken);
                        } else {
                            cb.error(new LoginFailedException(loginResult.getMessage()));
                        }
                    } else {
                        cb.error(new LoginFailedException(loginResult.getMessage()));
                    }
                } else {
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
            public void success(@NonNull Call<MwQueryResponse> call, int id, @NonNull Set<String> groups) {
                loginResult.setUserId(id);
                loginResult.setGroups(groups);
                cb.success(loginResult);

                L.v("Found user ID " + id + " for " + wiki.subdomain());
            }

            @Override
            public void failure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught) {
                L.e("Login succeeded but getting group information failed. " + caught);
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
        @POST("w/api.php?format=json&formatversion=2&action=query&meta=tokens&type=login")
        Call<MwQueryResponse> requestLoginToken();

        /** Actually log in. Has to be x-www-form-urlencoded */
        @NonNull
        @FormUrlEncoded
        @POST("w/api.php?action=clientlogin&format=json&formatversion=2&rememberMe=")
        Call<LoginResponse> logIn(@Field("username") String user, @Field("password") String pass,
                                  @Field("logintoken") String token, @Field("loginreturnurl") String url);

        /** Actually log in. Has to be x-www-form-urlencoded */
        @NonNull
        @FormUrlEncoded
        @POST("w/api.php?action=clientlogin&format=json&formatversion=2&rememberMe=")
        Call<LoginResponse> logIn(@Field("username") String user, @Field("password") String pass,
                                  @Field("OATHToken") String twoFactorCode, @Field("logintoken") String token,
                                  @Field("logincontinue") boolean loginContinue);
    }

    private static final class LoginResponse {
        @SuppressWarnings("unused") @SerializedName("error") @Nullable
        private MwServiceError error;

        @SuppressWarnings("unused") @SerializedName("clientlogin") @Nullable
        private ClientLogin clientLogin;

        @Nullable public MwServiceError getError() {
            return error;
        }

        @Nullable LoginResult toLoginResult(@NonNull WikiSite site, @NonNull String password) {
            return clientLogin != null ? clientLogin.toLoginResult(site, password) : null;
        }

        private static class ClientLogin {
            @SuppressWarnings("unused,NullableProblems") @NonNull private String status;
            @SuppressWarnings("unused") @Nullable private List<Request> requests;
            @SuppressWarnings("unused") @Nullable private String message;
            @SuppressWarnings("unused") @SerializedName("username") @Nullable private String userName;

            LoginResult toLoginResult(@NonNull WikiSite site, @NonNull String password) {
                String userMessage = message;
                if ("UI".equals(status)) {
                    if (requests != null) {
                        for (Request req : requests) {
                            if ("TOTPAuthenticationRequest".equals(req.id())) {
                                return new LoginOAuthResult(site, status, userName, password, message);
                            }
                        }
                    }
                } else if (!"PASS".equals(status) && !"FAIL".equals(status)) {
                    //TODO: String resource -- Looks like needed for others in this class too
                    userMessage = "An unknown error occurred.";
                }
                return new LoginResult(site, status, userName, password, userMessage);
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
