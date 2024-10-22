package org.wikipedia.push

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.notifications.PollNotificationWorker
import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L

class WikipediaFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        L.d("Message from: ${remoteMessage.from}")

        if (remoteMessage.data.containsValue(MESSAGE_TYPE_CHECK_ECHO)) {
            PollNotificationWorker.schedulePollNotificationJob(this)
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
        Prefs.pushNotificationTokenOld = Prefs.pushNotificationToken
        Prefs.pushNotificationToken = token
        Prefs.isPushNotificationTokenSubscribed = false

        updateSubscription()
    }

    companion object {
        const val MESSAGE_TYPE_CHECK_ECHO = "checkEchoV1"
        private const val SUBSCRIBE_RETRY_COUNT = 5
        private const val UNSUBSCRIBE_RETRY_COUNT = 3

        private var subscriptionJob: Job? = null

        fun isUsingPush(): Boolean {
            return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(WikipediaApp.instance) == ConnectionResult.SUCCESS &&
                    Prefs.pushNotificationToken.isNotEmpty() &&
                    Prefs.isPushNotificationTokenSubscribed
        }

        fun updateSubscription() {
            if (!AccountUtil.isLoggedIn) {
                // Don't bother doing anything if the user is not logged in.
                return
            }

            subscriptionJob?.cancel()

            subscriptionJob = MainScope().launch(CoroutineExceptionHandler { _, t ->
                L.e(t)
            }) {
                for (lang in WikipediaApp.instance.languageState.appLanguageCodes) {
                    val csrfToken = CsrfTokenClient.getToken(WikiSite.forLanguageCode(lang))
                    if (lang == WikipediaApp.instance.appOrSystemLanguageCode) {
                        subscribeWithCsrf(csrfToken)
                    }
                    setNotificationOptions(lang, csrfToken)
                }
            }
        }

        private suspend fun subscribeWithCsrf(csrfToken: String) {
            if (Prefs.isPushNotificationTokenSubscribed || Prefs.pushNotificationToken.isEmpty()) {
                // Don't do anything if the token is already subscribed, or if the token is empty.
                return
            }

            val token = Prefs.pushNotificationToken
            val oldToken = Prefs.pushNotificationTokenOld

            // Make sure to unsubscribe the previous token, if any
            if (oldToken.isNotEmpty()) {
                if (oldToken != token) {
                    try {
                        unsubscribePushToken(csrfToken, oldToken)
                        L.d("Previous token unsubscribed successfully.")
                        Prefs.pushNotificationTokenOld = ""
                    } catch (e: Exception) {
                        if (e is CancellationException) {
                            throw e
                        } else if (e is MwException && e.error.title == "echo-push-token-not-found") {
                            // token was not found in the database, so consider it gone.
                            Prefs.pushNotificationTokenOld = ""
                        } else {
                            L.e(e)
                        }
                    }
                } else {
                    Prefs.pushNotificationTokenOld = ""
                }
            }

            withContext(Dispatchers.IO) {
                for (i in 0 until SUBSCRIBE_RETRY_COUNT) {
                    try {
                        ServiceFactory.get(WikipediaApp.instance.wikiSite).subscribePush(csrfToken, token)
                        L.d("Token subscribed successfully.")
                        Prefs.isPushNotificationTokenSubscribed = true
                    } catch (e: Exception) {
                        if (e is CancellationException) {
                            throw e
                        } else if (e is MwException && e.error.title == "echo-push-token-exists") {
                            // token already exists in the database, so consider it subscribed.
                            Prefs.isPushNotificationTokenSubscribed = true
                        } else {
                            L.e(e)
                            continue
                        }
                    }
                    break
                }
            }
        }

        private suspend fun setNotificationOptions(lang: String, csrfToken: String) {
            if (Prefs.isPushNotificationOptionsSet) {
                return
            }

            val optionList = listOf(
                    "echo-subscriptions-push-edit-user-talk=1",
                    "echo-subscriptions-push-login-fail=1",
                    "echo-subscriptions-push-mention=1",
                    "echo-subscriptions-push-thank-you-edit=1",
                    "echo-subscriptions-push-reverted=1",
                    "echo-subscriptions-push-edit-thank=1",
                    "echo-cross-wiki-notifications=1"
            )

            ServiceFactory.get(WikiSite.forLanguageCode(lang)).postSetOptions(optionList.joinToString(separator = "|"), csrfToken)
            L.d("Notification options updated successfully.")
            Prefs.isPushNotificationOptionsSet = true
        }

        suspend fun unsubscribePushToken(csrfToken: String, pushToken: String): MwQueryResponse {
            if (pushToken.isEmpty()) {
                return MwQueryResponse()
            }
            return withContext(Dispatchers.IO) {
                for (i in 0 until UNSUBSCRIBE_RETRY_COUNT) {
                    try {
                        return@withContext ServiceFactory.get(WikipediaApp.instance.wikiSite).unsubscribePush(csrfToken, pushToken)
                    } catch (e: Exception) {
                        if (e is CancellationException) {
                            throw e
                        }
                        L.e(e)
                    }
                }
                MwQueryResponse()
            }
        }
    }
}
