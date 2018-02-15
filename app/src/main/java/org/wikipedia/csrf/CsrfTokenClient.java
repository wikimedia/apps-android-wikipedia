package org.wikipedia.csrf;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.widget.Toast;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.dataclient.SharedPreferenceCookieManager;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.WikiCachedService;
import org.wikipedia.login.LoginClient;
import org.wikipedia.login.LoginResult;
import org.wikipedia.util.log.L;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Headers;

public class CsrfTokenClient {
    static final String ANON_TOKEN = "+\\";
    private static final int MAX_RETRIES = 1;
    private static final int MAX_RETRIES_OF_LOGIN_BLOCKING = 2;
    @NonNull private final WikiCachedService<Service> cachedService = new MwCachedService<>(Service.class);
    @NonNull private final WikiSite csrfWikiSite;
    @NonNull private final WikiSite loginWikiSite;
    private int retries = 0;

    @Nullable private Call<MwQueryResponse> csrfTokenCall;
    @NonNull private LoginClient loginClient = new LoginClient();

    public CsrfTokenClient(@NonNull WikiSite csrfWikiSite, @NonNull WikiSite loginWikiSite) {
        this.csrfWikiSite = csrfWikiSite;
        this.loginWikiSite = loginWikiSite;
    }

    public void request(@NonNull final Callback callback) {
        request(false, callback);
    }

    public void request(boolean forceLogin, @NonNull final Callback callback) {
        cancel();
        if (forceLogin) {
            retryWithLogin(new RuntimeException("Forcing login..."), callback);
            return;
        }
        Service service = cachedService.service(csrfWikiSite);
        csrfTokenCall = request(service, callback);
    }

    public void cancel() {
        loginClient.cancel();
        if (csrfTokenCall != null) {
            csrfTokenCall.cancel();
            csrfTokenCall = null;
        }
    }

    @VisibleForTesting
    @NonNull
    Call<MwQueryResponse> request(@NonNull Service service, @NonNull final Callback cb) {
        return requestToken(service, new CsrfTokenClient.Callback() {
            @Override public void success(@NonNull String token) {
                if (AccountUtil.isLoggedIn() && token.equals(ANON_TOKEN)) {
                    retryWithLogin(new RuntimeException("App believes we're logged in, but got anonymous token."), cb);
                } else {
                    cb.success(token);
                }
            }

            @Override public void failure(@NonNull Throwable caught) {
                retryWithLogin(caught, cb);
            }

            @Override
            public void twoFactorPrompt() {
                cb.twoFactorPrompt();
            }
        });
    }

    private void retryWithLogin(@NonNull Throwable caught, @NonNull final Callback callback) {
        if (retries < MAX_RETRIES
                && !TextUtils.isEmpty(AccountUtil.getUserName())
                && !TextUtils.isEmpty(AccountUtil.getPassword())) {
            retries++;

            SharedPreferenceCookieManager.getInstance().clearAllCookies();

            login(AccountUtil.getUserName(), AccountUtil.getPassword(), () -> {
                L.i("retrying...");
                request(callback);
            }, callback);
        } else {
            callback.failure(caught);
        }
    }

    private void login(@NonNull final String username, @NonNull final String password,
                       @NonNull final RetryCallback retryCallback,
                       @NonNull final Callback callback) {
        new LoginClient().request(loginWikiSite, username, password,
                new LoginClient.LoginCallback() {
                    @Override
                    public void success(@NonNull LoginResult loginResult) {
                        if (loginResult.pass()) {
                            AccountUtil.updateAccount(null, loginResult);
                            retryCallback.retry();
                        } else {
                            callback.failure(new LoginClient.LoginFailedException(loginResult.getMessage()));
                        }
                    }

                    @Override
                    public void twoFactorPrompt(@NonNull Throwable caught, @Nullable String token) {
                        callback.twoFactorPrompt();
                    }

                    @Override
                    public void error(@NonNull Throwable caught) {
                        callback.failure(caught);
                    }
                });
    }

    @NonNull public String getTokenBlocking() throws Throwable {
        String token = "";
        Service service = cachedService.service(csrfWikiSite);

        for (int retry = 0; retry < MAX_RETRIES_OF_LOGIN_BLOCKING; retry++) {
            try {
                if (retry > 0) {
                    // Log in explicitly
                    new LoginClient().loginBlocking(loginWikiSite, AccountUtil.getUserName(),
                            AccountUtil.getPassword(), "");
                }

                Response<MwQueryResponse> response = service.request().execute();
                if (response.body() == null || !response.body().success()
                        || TextUtils.isEmpty(response.body().query().csrfToken())) {
                    continue;
                }
                token = response.body().query().csrfToken();
                if (AccountUtil.isLoggedIn() && token.equals(ANON_TOKEN)) {
                    throw new RuntimeException("App believes we're logged in, but got anonymous token.");
                }
                break;
            } catch (Throwable t) {
                L.w(t);
            }
        }
        if (TextUtils.isEmpty(token) || token.equals(ANON_TOKEN)) {
            throw new IOException("Invalid token, or login failure.");
        }
        return token;
    }

    @VisibleForTesting @NonNull Call<MwQueryResponse> requestToken(@NonNull Service service,
                                                                   @NonNull final Callback cb) {
        Call<MwQueryResponse> call = service.request();
        call.enqueue(new retrofit2.Callback<MwQueryResponse>() {
            @Override
            public void onResponse(Call<MwQueryResponse> call, Response<MwQueryResponse> response) {
                if (response.body().success()) {
                    // noinspection ConstantConditions
                    cb.success(response.body().query().csrfToken());
                } else if (response.body().hasError()) {
                    // noinspection ConstantConditions
                    cb.failure(new MwException(response.body().getError()));
                } else {
                    cb.failure(new IOException("An unknown error occurred."));
                }
            }

            @Override
            public void onFailure(Call<MwQueryResponse> call, Throwable t) {
                cb.failure(t);
            }
        });
        return call;
    }

    public interface Callback {
        void success(@NonNull String token);
        void failure(@NonNull Throwable caught);
        void twoFactorPrompt();
    }

    public static class DefaultCallback implements Callback {
        @Override
        public void success(@NonNull String token) {
        }

        @Override
        public void failure(@NonNull Throwable caught) {
            L.e(caught);
        }

        @Override
        public void twoFactorPrompt() {
            Toast.makeText(WikipediaApp.getInstance(),
                    R.string.login_2fa_other_workflow_error_msg, Toast.LENGTH_LONG).show();
        }
    }

    private interface RetryCallback {
        void retry();
    }

    @VisibleForTesting interface Service {
        @Headers("Cache-Control: no-cache")
        @GET("w/api.php?action=query&format=json&formatversion=2&meta=tokens&type=csrf")
        Call<MwQueryResponse> request();
    }
}
