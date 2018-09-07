package org.wikipedia.notifications;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.google.gson.JsonParseException;

import org.wikipedia.WikipediaApp;
import org.wikipedia.csrf.CsrfTokenClient;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.util.log.L;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

public final class NotificationClient {

    public interface Callback {
        void success(@NonNull List<Notification> notifications, @Nullable String continueStr);
        void failure(Throwable t);
    }

    public interface UnreadWikisCallback {
        void success(@NonNull Map<String, Notification.UnreadNotificationWikiItem> wikiMap);
        void failure(Throwable t);
    }

    public interface PollCallback {
        void success(@NonNull String lastNotificationTime);
        void failure(Throwable t);
    }

    public interface MarkReadCallback {
        void success(@NonNull String timeStamp);
        void failure(Throwable t);
    }

    @VisibleForTesting static class CallbackAdapter implements retrofit2.Callback<MwQueryResponse> {
        @NonNull private final Callback callback;

        CallbackAdapter(@NonNull Callback callback) {
            this.callback = callback;
        }

        @Override public void onResponse(@NonNull Call<MwQueryResponse> call, @NonNull Response<MwQueryResponse> response) {
            if (response.body() != null && response.body().query() != null
                    && response.body().query().notifications() != null) {
                // noinspection ConstantConditions
                callback.success(response.body().query().notifications().list(), response.body().query().notifications().getContinue());
            } else {
                callback.failure(new JsonParseException("Notification response is malformed."));
            }
        }

        @Override public void onFailure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught) {
            L.v(caught);
            callback.failure(caught);
        }
    }

    public void getUnreadNotificationWikis(@NonNull WikiSite wiki, @NonNull UnreadWikisCallback callback) {
        ServiceFactory.get(wiki).getUnreadNotificationWikis().enqueue(new retrofit2.Callback<MwQueryResponse>() {
            @Override
            public void onResponse(@NonNull Call<MwQueryResponse> call, @NonNull Response<MwQueryResponse> response) {
                if (response.body() != null && response.body().query() != null
                        && response.body().query().unreadNotificationWikis() != null) {
                    // noinspection ConstantConditions
                    callback.success(response.body().query().unreadNotificationWikis());
                } else {
                    callback.failure(new JsonParseException("Notification response is malformed."));
                }
            }

            @Override
            public void onFailure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught) {
                L.v(caught);
                callback.failure(caught);
            }
        });
    }

    public void getNotificationsWithForeignSummary(@NonNull WikiSite wiki, @NonNull final Callback callback) {
        ServiceFactory.get(wiki).getForeignSummary().enqueue(new CallbackAdapter(callback));
    }

    public void getAllNotifications(@NonNull WikiSite wiki, @NonNull final Callback callback, boolean displayArchived,
                                    @Nullable String continueStr, @Nullable String[] wikis) {
        ServiceFactory.get(wiki).getAllNotifications(wikis != null && wikis.length > 0 ? TextUtils.join("|", wikis) : "*",
                displayArchived ? "read" : "!read", TextUtils.isEmpty(continueStr) ? null : continueStr)
                .enqueue(new CallbackAdapter(callback));
    }

    public void getLastUnreadNotificationTime(@NonNull WikiSite wiki, @NonNull final PollCallback callback) {
        ServiceFactory.get(wiki).getLastUnreadNotification().enqueue(new retrofit2.Callback<MwQueryResponse>() {
            @Override
            public void onResponse(@NonNull Call<MwQueryResponse> call, @NonNull Response<MwQueryResponse> response) {
                if (response.body() != null && response.body().query() != null
                        && response.body().query().notifications() != null) {
                    String latestTime = "";
                    if (response.body().query().notifications().list() != null
                            && response.body().query().notifications().list().size() > 0) {
                        // noinspection ConstantConditions
                        for (Notification n : response.body().query().notifications().list()) {
                            if (n.getUtcIso8601().compareTo(latestTime) > 0) {
                                latestTime = n.getUtcIso8601();
                            }
                        }
                    }
                    callback.success(latestTime);
                } else {
                    callback.failure(new JsonParseException("Notification response is malformed."));
                }
            }

            @Override
            public void onFailure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught) {
                L.v(caught);
                callback.failure(caught);
            }
        });
    }

    public void markRead(@NonNull WikiSite wiki, @NonNull List<Notification> notifications) {
        markRead(wiki, notifications, false);
    }

    public void markUnread(@NonNull WikiSite wiki, @NonNull List<Notification> notifications) {
        markRead(wiki, notifications, true);
    }

    private void markRead(@NonNull WikiSite wiki, @NonNull List<Notification> notifications, boolean unread) {
        final String idListStr = TextUtils.join("|", notifications);
        CsrfTokenClient editTokenClient = new CsrfTokenClient(wiki, WikipediaApp.getInstance().getWikiSite());
        editTokenClient.request(new CsrfTokenClient.DefaultCallback() {
            @Override
            public void success(@NonNull String token) {
                ServiceFactory.get(wiki).markRead(token, unread ? null : idListStr, unread ? idListStr : null)
                        .enqueue(new retrofit2.Callback<MwQueryResponse>() {
                            @Override
                            public void onResponse(@NonNull Call<MwQueryResponse> call, @NonNull Response<MwQueryResponse> response) {
                                // don't care about the response for now.
                            }

                            @Override
                            public void onFailure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable t) {
                                L.e(t);
                            }
                        });
            }
        });
    }

    private void markSeen(@NonNull WikiSite wiki, @NonNull MarkReadCallback callback) {
        CsrfTokenClient tokenClient = new CsrfTokenClient(wiki, WikipediaApp.getInstance().getWikiSite());
        tokenClient.request(new CsrfTokenClient.DefaultCallback() {
            @Override
            public void success(@NonNull String token) {
                ServiceFactory.get(wiki).markSeen(token)
                        .enqueue(new retrofit2.Callback<MwQueryResponse>() {
                            @Override
                            public void onResponse(@NonNull Call<MwQueryResponse> call, @NonNull Response<MwQueryResponse> response) {
                                if (response.body() != null && response.body().query() != null
                                        && response.body().query().getEchoMarkSeen() != null && !TextUtils.isEmpty(response.body().query().getEchoMarkSeen().getTimestamp())) {
                                    callback.success(response.body().query().getEchoMarkSeen().getTimestamp());
                                } else {
                                    callback.failure(new JsonParseException("Notification response is malformed."));
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable t) {
                                callback.failure(t);
                            }
                        });
            }
        });
    }

    @VisibleForTesting @NonNull
    Call<MwQueryResponse> requestNotifications(@NonNull Service service, @NonNull String wikiList) {
        return service.getAllNotifications(wikiList, "!read", null);
    }
}
