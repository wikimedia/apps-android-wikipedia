package org.wikipedia.connectivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.net.ConnectivityManagerCompat;

import org.wikipedia.WikipediaApp;
import org.wikipedia.events.NetworkConnectEvent;

public class NetworkConnectivityReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            NetworkInfo networkInfo = getNetworkInfoFromBroadcast(context, intent);
            if (networkInfo != null && networkInfo.isConnected()) {
                post(new NetworkConnectEvent());
            }
        }
    }

    @Nullable private NetworkInfo getNetworkInfoFromBroadcast(Context context, Intent intent) {
        return ConnectivityManagerCompat.getNetworkInfoFromBroadcast(getConnectivityManager(context), intent);
    }

    @Nullable private ConnectivityManager getConnectivityManager(Context context) {
        return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    private void post(@NonNull Object event) {
        WikipediaApp.getInstance().getBus().post(event);
    }
}
