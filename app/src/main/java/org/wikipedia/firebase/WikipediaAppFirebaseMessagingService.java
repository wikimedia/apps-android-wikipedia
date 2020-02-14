package org.wikipedia.firebase;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.wikipedia.push.WikipediaAppPushServiceClient;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.log.L;

public class WikipediaAppFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        // TODO: Perform any special handling needed here
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        if (!token.equals(Prefs.getCurrentFirebaseToken())) {
            Prefs.setCurrentFirebaseToken(token);
            Prefs.setPushServiceSubscriberId(null);
            if (Prefs.getPushNotificationsEnabled()) {
                WikipediaAppPushServiceClient.getInstance(this).updateSubscriptionState();
            }
        }
        L.d("Received new token: " + token);
    }

}
