package org.wikipedia.connectivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.Nullable;
import android.support.v4.net.ConnectivityManagerCompat;

import org.wikipedia.WikipediaApp;
import org.wikipedia.events.NetworkConnectEvent;

import java.util.concurrent.TimeUnit;

public class NetworkConnectivityReceiver extends BroadcastReceiver {
    private static long ONLINE_CHECK_THRESHOLD_MILLIS = TimeUnit.MINUTES.toMillis(1);
    private boolean online = true;
    private long lastCheckedMillis;

    public boolean isOnline() {
        if (System.currentTimeMillis() - lastCheckedMillis > ONLINE_CHECK_THRESHOLD_MILLIS) {
            updateOnlineState();
            lastCheckedMillis = System.currentTimeMillis();
        }
        return online;
    }

    @Override public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            NetworkInfo networkInfo = getNetworkInfoFromBroadcast(context, intent);
            if (networkInfo != null && networkInfo.isConnected()) {
                WikipediaApp.getInstance().getBus().post(new NetworkConnectEvent());
            }
            updateOnlineState();
        }
    }

    private void updateOnlineState() {
        NetworkInfo info = getConnectivityManager(WikipediaApp.getInstance()).getActiveNetworkInfo();
        online = info != null && info.isConnected();
    }

    @Nullable private NetworkInfo getNetworkInfoFromBroadcast(Context context, Intent intent) {
        return ConnectivityManagerCompat.getNetworkInfoFromBroadcast(getConnectivityManager(context), intent);
    }

    @Nullable private ConnectivityManager getConnectivityManager(Context context) {
        return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }
}
