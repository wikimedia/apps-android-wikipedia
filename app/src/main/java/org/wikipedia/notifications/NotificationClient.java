package org.wikipedia.notifications;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.google.gson.JsonParseException;

import org.wikipedia.WikipediaApp;
import org.wikipedia.csrf.CsrfTokenClient;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.retrofit.RetrofitFactory;
import org.wikipedia.util.log.L;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public final class NotificationClient {
    @NonNull private final Service service;
    @NonNull private final CsrfTokenClient editTokenClient;

    public interface Callback {
        void success(@NonNull List<Notification> notifications);
        void failure(Throwable t);
    }

    private static final NotificationClient INSTANCE = new NotificationClient(new WikiSite("www.wikidata.org", ""));

    public static NotificationClient instance() {
        return INSTANCE;
    }

    private NotificationClient(@NonNull WikiSite wiki) {
        service = RetrofitFactory.newInstance(wiki).create(Service.class);
        editTokenClient = new CsrfTokenClient(wiki, WikipediaApp.getInstance().getWikiSite());
    }

    @VisibleForTesting static class CallbackAdapter implements retrofit2.Callback<MwQueryResponse> {
        @NonNull private final Callback callback;

        CallbackAdapter(@NonNull Callback callback) {
            this.callback = callback;
        }

        @Override public void onResponse(Call<MwQueryResponse> call, Response<MwQueryResponse> response) {
            if (response.body() != null && response.body().query() != null) {
                // noinspection ConstantConditions
                callback.success(response.body().query().notifications());
            } else {
                callback.failure(new JsonParseException("Notification response is malformed."));
            }
        }

        @Override public void onFailure(Call<MwQueryResponse> call, Throwable caught) {
            L.v(caught);
            callback.failure(caught);
        }
    }

    /**
     * Obrain a list of unread notifications for the user who is currently logged in.
     * @param callback Callback that will receive the list of notifications.
     * @param wikis List of wiki names for which notifications should be received. These must be
     *              in the "DB name" format, as in "enwiki", "zhwiki", "wikidatawiki", etc.
     */
    public void getNotifications(@NonNull final Callback callback, @NonNull String... wikis) {
        String wikiList = TextUtils.join("|", wikis);
        requestNotifications(service, wikiList).enqueue(new CallbackAdapter(callback));
    }

    public void markRead(List<Notification> notifications) {
        final String idListStr = TextUtils.join("|", notifications);
        editTokenClient.request(new CsrfTokenClient.DefaultCallback() {
            @Override
            public void success(@NonNull String token) {
                requestMarkRead(service, token, idListStr).enqueue(new retrofit2.Callback<MwQueryResponse>() {
                    @Override
                    public void onResponse(Call<MwQueryResponse> call, Response<MwQueryResponse> response) {
                        // don't care about the response for now.
                    }

                    @Override
                    public void onFailure(Call<MwQueryResponse> call, Throwable t) {
                        L.e(t);
                    }
                });
            }
        });
    }

    @VisibleForTesting @NonNull
    Call<MwQueryResponse> requestNotifications(@NonNull Service service, @NonNull String wikiList) {
        return service.getNotifications(wikiList);
    }

    @VisibleForTesting @NonNull
    Call<MwQueryResponse> requestMarkRead(@NonNull Service service, @NonNull String token, @NonNull String idList) {
        return service.markRead(token, idList);
    }

    @VisibleForTesting interface Service {
        String ACTION = "w/api.php?format=json&formatversion=2&action=";

        @Headers("Cache-Control: no-cache")
        @GET(ACTION + "query&meta=notifications&notfilter=!read&notprop=list")
        @NonNull
        Call<MwQueryResponse> getNotifications(@Query("notwikis") @NonNull String wikiList);

        @FormUrlEncoded
        @Headers("Cache-Control: no-cache")
        @POST(ACTION + "echomarkread")
        Call<MwQueryResponse> markRead(@Field("token") @NonNull String token, @Field("list") @NonNull String idList);
    }
}
