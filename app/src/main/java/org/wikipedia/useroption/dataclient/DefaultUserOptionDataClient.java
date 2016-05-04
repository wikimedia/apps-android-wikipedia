package org.wikipedia.useroption.dataclient;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.retrofit.RetrofitException;
import org.wikipedia.dataclient.retrofit.RetrofitFactory;
import org.wikipedia.dataclient.mwapi.MwPostResponse;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.editing.FetchEditTokenTask;
import org.wikipedia.useroption.UserOption;

import java.util.concurrent.Executor;

import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public class DefaultUserOptionDataClient implements UserOptionDataClient {
    @NonNull private final Site site;
    @NonNull private final Client client;

    public DefaultUserOptionDataClient(@NonNull Site site) {
        this.site = site;
        client = RetrofitFactory.newInstance(site).create(Client.class);
    }

    @NonNull
    @Override
    public UserInfo get() {
        return client.get().query().userInfo();
    }

    @Override
    public void post(@NonNull UserOption option) {
        client.post(getToken(), option.key(), option.val()).check(site);
    }

    @Override
    public void delete(@NonNull String key) {
        client.delete(getToken(), key).check(site);
    }

    @NonNull private String getToken() {
        if (app().getEditTokenStorage().token(site) == null) {
            requestToken();
        }

        String token = app().getEditTokenStorage().token(site);
        if (token == null) {
            throw RetrofitException.unexpectedError(
                    new RuntimeException("No token for " + site.authority()));
        }
        return token;
    }

    private void requestToken() {
        new FetchEditTokenTask(app(), site) {
            @Override
            public void onFinish(String result) {
                app().getEditTokenStorage().token(site, result);
            }

            @Override
            public void execute() {
                super.executeOnExecutor(new SynchronousExecutor());
            }
        }.execute();
    }

    private static WikipediaApp app() {
        return WikipediaApp.getInstance();
    }

    private static class SynchronousExecutor implements Executor {
        @Override
        public void execute(@NonNull Runnable runnable) {
            runnable.run();
        }
    }

    private interface Client {
        String ACTION = "w/api.php?format=json&formatversion=2&action=";

        @GET(ACTION + "query&meta=userinfo&uiprop=options")
        @NonNull MwQueryResponse<QueryUserInfo> get();

        @FormUrlEncoded
        @POST(ACTION + "options")
        @NonNull PostResponse post(@Field("token") @NonNull String token,
                                   @Query("optionname") @NonNull String key,
                                   @Query("optionvalue") @Nullable String value);

        @FormUrlEncoded
        @POST(ACTION + "options")
        @NonNull PostResponse delete(@Field("token") @NonNull String token,
                                     @Query("change") @NonNull String key);
    }

    private static class PostResponse extends MwPostResponse {
        private String options;

        public String result() {
            return options;
        }

        public void check(@NonNull Site site) {
            if (!success(options)) {
                if (badToken()) {
                    app().getEditTokenStorage().token(site, null);
                }

                throw RetrofitException.unexpectedError(
                        new RuntimeException("Bad response for site " + site.host()
                                + " = " + result()));
            }
        }
    }

    private static class QueryUserInfo {
        @SerializedName("userinfo")
        private UserInfo userInfo;

        public UserInfo userInfo() {
            return userInfo;
        }
    }
}
