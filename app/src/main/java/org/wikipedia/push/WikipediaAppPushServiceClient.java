package org.wikipedia.push;

import android.content.Context;

import androidx.annotation.NonNull;

import org.wikipedia.dataclient.PushService;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.firebase.FirebaseStateManager;
import org.wikipedia.language.AppLanguageState;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.Callback;
import org.wikipedia.util.log.L;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;


public final class WikipediaAppPushServiceClient {

    @NonNull private PushService pushService = ServiceFactory.getPush();
    @NonNull private CompositeDisposable disposables = new CompositeDisposable();
    @NonNull private Context context;

    public static WikipediaAppPushServiceClient getInstance(@NonNull Context context) {
        return new WikipediaAppPushServiceClient(context);
    }

    private WikipediaAppPushServiceClient(@NonNull Context context) {
        this.context = context;
    }

    public void updateSubscriptionState() {
        FirebaseStateManager.getCurrentToken(new FirebaseTokenCallback(context));
    }

    public void deleteSubscription() {
        String subscriberId = Prefs.getPushServiceSubscriberId();
        if (subscriberId == null) {
            L.d("No subscriber ID to delete!");
            return;
        }
        disposables.add(pushService.deleteSubscription(subscriberId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        // 204
                        rsp -> {
                            Prefs.setPushServiceSubscriberId(null);
                            L.d("Subscription deleted");
                        },
                        err -> L.e("Error deleting push service subscription", err)
                ));
    }

    private final class FirebaseTokenCallback implements Callback<String, Exception> {
        private Context context;

        private FirebaseTokenCallback(@NonNull Context context) {
            this.context = context;
        }

        @Override
        public void onSuccess(String token) {
            Prefs.setCurrentFirebaseToken(token);
            L.d("Received Firebase token: " + token);

            String subscriberId = Prefs.getPushServiceSubscriberId();
            AppLanguageState appLanguageState = new AppLanguageState(context);
            String lang = appLanguageState.getAppLanguageCode();

            if (subscriberId == null) {
                disposables.add(pushService.subscribe(PushService.PROTOCOL, lang, token)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                rsp -> {
                                    String id = rsp.id();
                                    if (id != null) {
                                        Prefs.setPushServiceSubscriberId(id);
                                        L.d("Found subscriber ID: " + id);
                                    }
                                },
                                err -> L.e("Error subscribing to push service", err)
                        )
                );
            } else {
                disposables.add(pushService.updateSubscription(subscriberId, lang)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                rsp -> L.d("Subscriber info updated"), // 204
                                // TODO: If this 404s, the app's subscriber ID value is invalid
                                //  (perhaps because the server has changed). Clear it and
                                //  resubscribe.
                                err -> L.e("Error updating push service subscription", err)
                        ));
            }
        }

        @Override
        public void onFailure(Exception e) {
            L.e("Failed to retrieve the current Firebase token", e);
        }
    }


}
