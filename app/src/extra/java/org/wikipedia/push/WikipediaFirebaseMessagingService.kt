package org.wikipedia.push

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.notifications.NotificationPollBroadcastReceiver
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ThrowableUtil
import org.wikipedia.util.log.L

class WikipediaFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        L.d("Message from: ${remoteMessage.from}")

        if (remoteMessage.data.containsValue(MESSAGE_TYPE_CHECK_ECHO)) {
            handleCheckEcho()
        }

        // The message could also contain a notification payload, but that's not how we're using it.
        // remoteMessage.notification?.let {
        //    ...
        // }
    }

    // Called when a token is first generated for the app, or when a token is revoked and
    // regenerated for security reasons.
    override fun onNewToken(token: String) {
        L.d("Received a new Firebase token...")

        // As soon as we receive a new token, it's super important to save it in our Prefs, because
        // another one might not be generated for a long time, and we should preserve it in case
        // the subscription API happens to fail.
        Prefs.setPushNotificationTokenOld(Prefs.getPushNotificationToken())
        Prefs.setPushNotificationToken(token)
        Prefs.setPushNotificationTokenSubscribed(false)

        updateSubscription()
    }

    private fun handleCheckEcho() {
        if (!Prefs.notificationPollEnabled()) {
            return
        }
        NotificationPollBroadcastReceiver.pollNotifications(this)
    }

    companion object {
        const val MESSAGE_TYPE_CHECK_ECHO = "checkEchoV1"
        private const val SUBSCRIBE_RETRY_COUNT = 5
        private const val UNSUBSCRIBE_RETRY_COUNT = 3
        private var csrfDisposable: Disposable? = null

        fun isUsingPush(): Boolean {
            return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(WikipediaApp.getInstance()) == ConnectionResult.SUCCESS &&
                    Prefs.getPushNotificationToken().isNotEmpty() &&
                    Prefs.isPushNotificationTokenSubscribed()
        }

        fun updateSubscription() {
            if (!AccountUtil.isLoggedIn) {
                // Don't bother doing anything if the user is not logged in.
                return
            }

            csrfDisposable?.dispose()
            csrfDisposable = CsrfTokenClient(WikipediaApp.getInstance().wikiSite).token
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        subscribeWithCsrf(it)
                        setNotificationOptions(it)
                    }, {
                        L.e(it)
                    })
        }

        private fun subscribeWithCsrf(csrfToken: String) {
            if (Prefs.isPushNotificationTokenSubscribed() || Prefs.getPushNotificationToken().isEmpty()) {
                // Don't do anything if the token is already subscribed, or if the token is empty.
                return
            }

            val token = Prefs.getPushNotificationToken()
            val oldToken = Prefs.getPushNotificationTokenOld()

            // Make sure to unsubscribe the previous token, if any
            if (oldToken.isNotEmpty()) {
                if (oldToken != token) {
                    unsubscribePushToken(csrfToken, oldToken)
                            .subscribe({
                                L.d("Previous token unsubscribed successfully.")
                                Prefs.setPushNotificationTokenOld("")
                            }, {
                                L.e(it)
                                val mwException = ThrowableUtil.getMwException(it)
                                if (mwException != null && mwException.error.title == "echo-push-token-not-found") {
                                    // token was not found in the database, so consider it gone.
                                    Prefs.setPushNotificationTokenOld("")
                                }
                            })
                } else {
                    Prefs.setPushNotificationTokenOld("")
                }
            }

            ServiceFactory.get(WikipediaApp.getInstance().wikiSite).subscribePush(csrfToken, token)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .retry(SUBSCRIBE_RETRY_COUNT.toLong())
                    .subscribe({
                        L.d("Token subscribed successfully.")
                        Prefs.setPushNotificationTokenSubscribed(true)
                    }, {
                        L.e(it)
                        if (it is MwException && it.error.title == "echo-push-token-exists") {
                            // token already exists in the database, so consider it subscribed.
                            Prefs.setPushNotificationTokenSubscribed(true)
                        }
                    })
        }

        private fun setNotificationOptions(csrfToken: String) {
            val optionList = ArrayList<String>()

            optionList.add("echo-subscriptions-push-edit-user-talk=" + if (Prefs.notificationUserTalkEnabled()) "1" else "0")
            optionList.add("echo-subscriptions-push-login-fail=" + if (Prefs.notificationLoginFailEnabled()) "1" else "0")
            optionList.add("echo-subscriptions-push-mention=" + if (Prefs.notificationMentionEnabled()) "1" else "0")
            optionList.add("echo-subscriptions-push-thank-you-edit=" + if (Prefs.notificationMilestoneEnabled()) "1" else "0")
            optionList.add("echo-subscriptions-push-reverted=" + if (Prefs.notificationRevertEnabled()) "1" else "0")
            optionList.add("echo-subscriptions-push-edit-thank=" + if (Prefs.notificationThanksEnabled()) "1" else "0")
            // Explicitly enable cross-wiki notifications
            optionList.add("echo-cross-wiki-notifications=1")

            ServiceFactory.get(WikipediaApp.getInstance().wikiSite).postSetOptions(optionList.joinToString(separator = "|"), csrfToken)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        L.d("Notification options successfully.")
                    }, {
                        L.e(it)
                    })
        }

        fun unsubscribePushToken(csrfToken: String, pushToken: String): Observable<MwQueryResponse> {
            if (pushToken.isEmpty()) {
                return Observable.just(MwQueryResponse())
            }
            return ServiceFactory.get(WikipediaApp.getInstance().wikiSite).unsubscribePush(csrfToken, pushToken)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .retry(UNSUBSCRIBE_RETRY_COUNT.toLong())
        }
    }
}
