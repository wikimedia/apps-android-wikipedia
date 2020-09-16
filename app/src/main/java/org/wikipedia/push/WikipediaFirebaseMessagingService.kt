package org.wikipedia.push

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.wikipedia.util.log.L


class WikipediaFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ

        L.d("Message from: ${remoteMessage.from}")

        // Check if message contains a data payload.
        remoteMessage.data.isNotEmpty().let {
            L.d("Message data payload: " + remoteMessage.data)
            // If the data needs to be processed in a long-running task, spin up a job using WorkManager.
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            L.d("Message Notification Body: ${it.body}")
        }

        // TODO: show the notification!
        // remoteMessage.notification?...

    }

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        L.d("Refreshed token: $token")

        // TODO: send token to our servers to associate the token with the current user.

    }

    companion object {
        fun subscribe() {
            // TODO: is it necessary to do this?
            FirebaseMessaging.getInstance().subscribeToTopic("wikipedia")
                    .addOnFailureListener {
                        L.e(it)
                    }
                    .addOnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            L.d("Failed to subscribe to FCM.")
                            return@addOnCompleteListener
                        }
                        L.d("Subscribed to FCM successfully.")
                    }
        }
    }
}
