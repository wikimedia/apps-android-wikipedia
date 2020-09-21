package org.wikipedia.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
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

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        L.d("Received a new Firebase token...")

        // Make sure to unsubscribe the previous token, if any
        if (Prefs.getPushNotificationToken().isNotEmpty()) {
            ServiceFactory.get(WikipediaApp.getInstance().wikiSite).unsubscribePush(Prefs.getPushNotificationToken())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .retry(UNSUBSCRIBE_RETRY_COUNT.toLong())
                    .subscribe({

                        L.d("Previous token unsubscribed successfully.")

                    }, {
                        L.e(it)
                    })
        }

        ServiceFactory.get(WikipediaApp.getInstance().wikiSite).subscribePush(token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .retry(SUBSCRIBE_RETRY_COUNT.toLong())
                .subscribe({

                    Prefs.setPushNotificationToken(token)

                }, {
                    L.e(it)
                })
    }

    private fun handleCheckEcho() {
        if (!Prefs.notificationPollEnabled()) {
            return
        }
        NotificationPollBroadcastReceiver.pollNotifications(this)
    }

    companion object {
        const val MESSAGE_TYPE_CHECK_ECHO = "checkEchoV1"
        const val SUBSCRIBE_RETRY_COUNT = 5
        const val UNSUBSCRIBE_RETRY_COUNT = 3
    }
}
