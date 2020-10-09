package org.wikipedia.push

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.notifications.NotificationPollBroadcastReceiver
import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L

class WikipediaFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        L.d("Message from: ${remoteMessage.from}")

        remoteMessage.data.isNotEmpty().let {
            L.d("Message data payload: " + remoteMessage.data)

            if (remoteMessage.data.containsValue(MESSAGE_TYPE_CHECK_ECHO)) {
                handleCheckEcho()
            }
        }

        // The message could also contain a notification payload, but that's not how we're using it.
        //remoteMessage.notification?.let {
        //    ...
        //}
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

        subscribeCurrentToken()
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

        fun isUsingPush(): Boolean {
            return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(WikipediaApp.getInstance()) == ConnectionResult.SUCCESS
                    && Prefs.getPushNotificationToken().isNotEmpty()
                    && Prefs.isPushNotificationTokenSubscribed()
        }

        fun subscribeCurrentToken() {
            if (!AccountUtil.isLoggedIn()
                    || Prefs.isPushNotificationTokenSubscribed()
                    || Prefs.getPushNotificationToken().isEmpty()) {
                // Don't bother registering the token if the user is not logged in,
                // or if the token was already subscribed successfully.
                return
            }

            CsrfTokenClient(WikipediaApp.getInstance().wikiSite).request(false, object : CsrfTokenClient.Callback {
                override fun success(token: String) {
                    subscribeWithCsrf(token)
                }

                override fun failure(t: Throwable) {
                    L.e(t)
                }

                override fun twoFactorPrompt() {
                    // ignore
                }
            })
        }

        private fun subscribeWithCsrf(csrfToken: String) {
            val token = Prefs.getPushNotificationToken()
            val oldToken = Prefs.getPushNotificationTokenOld()

            // Make sure to unsubscribe the previous token, if any
            if (oldToken.isNotEmpty()) {
                if (oldToken != token) {
                    ServiceFactory.get(WikipediaApp.getInstance().wikiSite).unsubscribePush(csrfToken, Prefs.getPushNotificationToken())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .retry(UNSUBSCRIBE_RETRY_COUNT.toLong())
                            .subscribe({
                                L.d("Previous token unsubscribed successfully.")
                                Prefs.setPushNotificationTokenOld("")
                            }, {
                                L.e(it)
                                if (it is MwException && it.error.title == "echo-push-token-not-found") {
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

        fun unsubscribePush(csrfToken: String): Observable<Any> {
            Prefs.setPushNotificationTokenOld("")
            Prefs.setPushNotificationTokenSubscribed(false)
            return ServiceFactory.get(WikipediaApp.getInstance().wikiSite).unsubscribePush(csrfToken, Prefs.getPushNotificationToken())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .retry(UNSUBSCRIBE_RETRY_COUNT.toLong()) as Observable<Any>
        }
    }
}
