package org.wikipedia.login;

import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.mwapi.MwResponse;
import org.wikipedia.json.GsonUtil;
import org.wikipedia.util.log.L;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Responsible for making login related requests to the server.
 */
public class LoginClient {
    private final CompositeDisposable disposables = new CompositeDisposable();
    public interface LoginCallback {
        void success(@NonNull LoginResult result);
        void twoFactorPrompt(@NonNull Throwable caught, @NonNull String token);
        void passwordResetPrompt(@Nullable String token);
        void error(@NonNull Throwable caught);
    }

    public void request(@NonNull final WikiSite wiki, @NonNull final String userName,
                        @NonNull final String password, @NonNull final LoginCallback cb) {
        cancel();
        disposables.add(getLoginToken(wiki)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(loginToken -> login(wiki, userName, password, null, null, loginToken, cb), cb::error));
    }

    void login(@NonNull final WikiSite wiki, @NonNull final String userName, @NonNull final String password,
               @Nullable final String retypedPassword, @Nullable final String twoFactorCode,
               @NonNull final String loginToken, @NonNull final LoginCallback cb) {

        disposables.add(getLoginResponse(wiki, userName, password, retypedPassword, twoFactorCode, loginToken)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(loginResponse -> {
                    LoginResult loginResult = loginResponse.toLoginResult(wiki, password);
                    if (loginResult != null) {
                        if (loginResult.pass() && !TextUtils.isEmpty(loginResult.getUserName())) {
                            return getExtendedInfo(wiki, loginResult);
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
                    return Observable.empty();
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(loginResult -> {
                    if (loginResult != null) {
                        cb.success(loginResult);
                    } else {
                        cb.error(new Throwable("Login succeeded but getting group information failed. "));
                    }
                }, caught -> {
                    L.e("Login process failed. " + caught);
                    cb.error(caught);
                }));
    }

    public Observable<LoginResponse> loginBlocking(@NonNull final WikiSite wiki, @NonNull final String userName,
                              @NonNull final String password, @Nullable final String twoFactorCode) {
        return getLoginToken(wiki)
                .flatMap(loginToken -> getLoginResponse(wiki, userName, password, null, twoFactorCode, loginToken))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(loginResponse -> {
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
                        }
                        throw new LoginFailedException(loginResult.getMessage());
                    } else if (!loginResult.pass() || TextUtils.isEmpty(loginResult.getUserName())) {
                        throw new LoginFailedException(loginResult.getMessage());
                    }
                    return loginResponse;
                });
    }

    private Observable<String> getLoginToken(@NonNull final WikiSite wiki) {
        return ServiceFactory.get(wiki).getLoginToken()
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    MwQueryResponse queryResponse = GsonUtil.getDefaultGson().fromJson(response, MwQueryResponse.class);
                    String loginToken = queryResponse.query().loginToken();
                    if (TextUtils.isEmpty(loginToken)) {
                        throw new RuntimeException("Received empty login token: " + GsonUtil.getDefaultGson().toJson(response));
                    }
                    return loginToken;
                });
    }

    private Observable<LoginResponse> getLoginResponse(@NonNull final WikiSite wiki,
                                                       @NonNull final String userName,
                                                       @NonNull final String password,
                                                       @Nullable final String retypedPassword,
                                                       @Nullable final String twoFactorCode,
                                                       @Nullable final String loginToken) {
        return TextUtils.isEmpty(twoFactorCode) && TextUtils.isEmpty(retypedPassword)
                ? ServiceFactory.get(wiki).postLogIn(userName, password, loginToken, Service.WIKIPEDIA_URL)
                : ServiceFactory.get(wiki).postLogIn(userName, password, retypedPassword, twoFactorCode, loginToken, true);
    }

    private Observable<LoginResult> getExtendedInfo(@NonNull final WikiSite wiki, @NonNull final LoginResult loginResult) {
        return ServiceFactory.get(wiki).getUserInfo()
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    int id = response.query().userInfo().id();
                    loginResult.setUserId(id);
                    loginResult.setGroups(response.query().userInfo().getGroups());
                    L.v("Found user ID " + id + " for " + wiki.subdomain());
                    return loginResult;
                });
    }

    public void cancel() {
        disposables.clear();
    }

    public static final class LoginResponse extends MwResponse {
        @SuppressWarnings("unused") @SerializedName("clientlogin") @Nullable
        private ClientLogin clientLogin;

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
                            if (req.id().endsWith("TOTPAuthenticationRequest")) {
                                return new LoginOAuthResult(site, status, userName, password, message);
                            } else if (req.id().endsWith("PasswordAuthenticationRequest")) {
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

            @NonNull String id() {
                return StringUtils.defaultString(id);
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
