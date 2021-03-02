package org.wikipedia.connectivity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import androidx.core.net.ConnectivityManagerCompat
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.EventPlatformClient
import org.wikipedia.events.NetworkConnectEvent
import java.util.concurrent.TimeUnit

class NetworkConnectivityReceiver : BroadcastReceiver() {
    private var online = true
    private var lastCheckedMillis: Long = 0

    fun isOnline(): Boolean {
        if (System.currentTimeMillis() - lastCheckedMillis > ONLINE_CHECK_THRESHOLD_MILLIS) {
            updateOnlineState()
            lastCheckedMillis = System.currentTimeMillis()
        }
        return online
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION == intent.action) {
            val networkInfo = ConnectivityManagerCompat.getNetworkInfoFromBroadcast(context.getSystemService()!!, intent)
            updateOnlineState()
            if (networkInfo != null && networkInfo.isConnected) {
                WikipediaApp.getInstance().bus.post(NetworkConnectEvent())
            }
        }
    }

    private fun updateOnlineState() {
        val info = WikipediaApp.getInstance().getSystemService<ConnectivityManager>()?.activeNetworkInfo
        online = info != null && info.isConnected
        EventPlatformClient.setEnabled(online)
    }

    companion object {
        private val ONLINE_CHECK_THRESHOLD_MILLIS = TimeUnit.MINUTES.toMillis(1)
    }
}
