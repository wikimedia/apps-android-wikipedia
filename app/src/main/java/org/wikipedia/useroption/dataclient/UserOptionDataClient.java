package org.wikipedia.useroption.dataclient;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.WikipediaApp;
import org.wikipedia.csrf.CsrfTokenClient;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.UserInfo;
import org.wikipedia.util.log.L;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

@SuppressLint("CheckResult")
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
        new CsrfTokenClient(wiki, WikipediaApp.getInstance().getWikiSite()).request(new CsrfTokenClient.DefaultCallback() {
            @Override
            public void success(@NonNull String token) {
                ServiceFactory.get(wiki).getUserOptions()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(response -> callback.success(response.query().userInfo()),
                                L::e);
            }
        });
    }

    public void post(@NonNull String key, @NonNull String val, @Nullable final UserOptionPostCallback callback) {
        new CsrfTokenClient(wiki, WikipediaApp.getInstance().getWikiSite()).request(new CsrfTokenClient.DefaultCallback() {
            @Override
            public void success(@NonNull String token) {
                ServiceFactory.get(wiki).postUserOption(token, key, val)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(response -> {
                            if (!response.success(response.getOptions())) {
                                L.e("Bad response for wiki " + wiki.authority() + " = " + response.getOptions());
                            } else if (callback != null) {
                                callback.success();
                            }
                        }, caught -> {
                            L.e(caught);
                            if (callback != null) {
                                callback.failure(caught);
                            }
                        });
            }
        });
    }

    public void delete(@NonNull final String key, @Nullable final UserOptionPostCallback callback) {
        new CsrfTokenClient(wiki, WikipediaApp.getInstance().getWikiSite()).request(new CsrfTokenClient.DefaultCallback() {
            @Override
            public void success(@NonNull String token) {
                ServiceFactory.get(wiki).deleteUserOption(token, key)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(response -> {
                            if (!response.success(response.getOptions())) {
                                L.e("Bad response for wiki " + wiki.authority() + " = " + response.getOptions());
                            } else if (callback != null) {
                                callback.success();
                            }
                        }, caught -> {
                            L.e(caught);
                            if (callback != null) {
                                callback.failure(caught);
                            }
                        });
            }
        });
    }
}
