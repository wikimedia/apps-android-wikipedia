package org.wikipedia.login;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.ListUserResponse;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.mwapi.MwServiceError;
import org.wikipedia.util.log.L;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Responsible for making login related requests to the server.
 */
public class LoginClient {
    @Nullable private Call<MwQueryResponse> tokenCall;
    @Nullable private Call<LoginResponse> loginCall;

    public interface LoginCallback {
        void success(@NonNull LoginResult result);
        void twoFactorPrompt(@NonNull Throwable caught, @Nullable String token);
        void passwordResetPrompt(@Nullable String token);
        void error(@NonNull Throwable caught);
    }

    public void request(@NonNull final WikiSite wiki, @NonNull final String userName,
                        @NonNull final String password, @NonNull final LoginCallback cb) {
        cancel();

        tokenCall = ServiceFactory.get(wiki).getLoginToken();
        tokenCall.enqueue(new Callback<MwQueryResponse>() {
            @Override public void onResponse(@NonNull Call<MwQueryResponse> call,
                                             @NonNull Response<MwQueryResponse> response) {
                login(wiki, userName, password, null, null, response.body().query().loginToken(), cb);
            }

            @Override
            public void onFailure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught) {
                if (call.isCanceled()) {
                    return;
                }
                cb.error(caught);
            }
        });
    }

    void login(@NonNull final WikiSite wiki, @NonNull final String userName, @NonNull final String password,
               @Nullable final String retypedPassword, @Nullable final String twoFactorCode,
               @Nullable final String loginToken, @NonNull final LoginCallback cb) {
        loginCall = TextUtils.isEmpty(twoFactorCode) && TextUtils.isEmpty(retypedPassword)
                ? ServiceFactory.get(wiki).postLogIn(userName, password, loginToken, Service.WIKIPEDIA_URL)
                : ServiceFactory.get(wiki).postLogIn(userName, password, retypedPassword, twoFactorCode, loginToken, true);
        loginCall.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
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
                        } else if (loginResult instanceof LoginResetPasswordResult) {
                            cb.passwordResetPrompt(loginToken);
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
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                if (call.isCanceled()) {
                    return;
                }
                cb.error(t);
            }
        });
    }

    public void loginBlocking(@NonNull final WikiSite wiki, @NonNull final String userName,
                              @NonNull final String password, @Nullable final String twoFactorCode) throws Throwable {
        Response<MwQueryResponse> tokenResponse = ServiceFactory.get(wiki).getLoginToken().execute();
        if (tokenResponse.body() == null || TextUtils.isEmpty(tokenResponse.body().query().loginToken())) {
            throw new IOException("Unexpected response when getting login token.");
        }
        String loginToken = tokenResponse.body().query().loginToken();

        Call<LoginResponse> tempLoginCall = StringUtils.defaultIfEmpty(twoFactorCode, "").isEmpty()
                ? ServiceFactory.get(wiki).postLogIn(userName, password, loginToken, Service.WIKIPEDIA_URL)
                : ServiceFactory.get(wiki).postLogIn(userName, password, null, twoFactorCode, loginToken, true);
        Response<LoginResponse> response = tempLoginCall.execute();
        LoginResponse loginResponse = response.body();
        if (loginResponse == null) {
            throw new IOException("Unexpected response when logging in.");
        }
        LoginResult loginResult = loginResponse.toLoginResult(wiki, password);
        if (loginResult == null) {
            throw new IOException("Unexpected response when logging in.");
        }
        if ("UI".equals(loginResult.getStatus())) {
            if (loginResult instanceof LoginOAuthResult) {

                // TODO: Find a better way to boil up the warning about 2FA
                Toast.makeText(WikipediaApp.getInstance(),
                        R.string.login_2fa_other_workflow_error_msg, Toast.LENGTH_LONG).show();

                throw new LoginFailedException(loginResult.getMessage());

            } else {
                throw new LoginFailedException(loginResult.getMessage());
            }
        } else if (!loginResult.pass() || TextUtils.isEmpty(loginResult.getUserName())) {
            throw new LoginFailedException(loginResult.getMessage());
        }
    }

    @SuppressLint("CheckResult")
    private void getExtendedInfo(@NonNull final WikiSite wiki, @NonNull String userName,
                                 @NonNull final LoginResult loginResult, @NonNull final LoginCallback cb) {
        ServiceFactory.get(wiki).getUserInfo(userName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    ListUserResponse user = response.query().getUserResponse(userName);
                    int id = response.query().userInfo().id();
                    loginResult.setUserId(id);
                    loginResult.setGroups(user.getGroups());
                    cb.success(loginResult);
                    L.v("Found user ID " + id + " for " + wiki.subdomain());
                }, caught -> {
                    L.e("Login succeeded but getting group information failed. " + caught);
                    cb.error(caught);
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

    public static final class LoginResponse {
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
                            } else if ("MediaWiki\\Auth\\PasswordAuthenticationRequest".equals(req.id())) {
                                return new LoginResetPasswordResult(site, status, userName, password, message);
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
