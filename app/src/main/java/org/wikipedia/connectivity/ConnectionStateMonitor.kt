package org.wikipedia.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.EventPlatformClient
import org.wikipedia.events.NetworkConnectEvent
import java.util.concurrent.TimeUnit


class ConnectionStateMonitor : ConnectivityManager.NetworkCallback() {

    private lateinit var connectivityManager: ConnectivityManager
    private var online = true
    private var lastCheckedMillis: Long = 0
    private var networkRequest = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .build()

    fun enable(context: Context) {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerNetworkCallback(networkRequest, this)
    }

    fun isOnline(): Boolean {
        if (System.currentTimeMillis() - lastCheckedMillis > ONLINE_CHECK_THRESHOLD_MILLIS) {
            updateOnlineState()
            lastCheckedMillis = System.currentTimeMillis()
        }
        return online
    }

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        updateOnlineState()
    }

    private fun updateOnlineState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            capabilities?.let {
                online = if (it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    true
                } else if (it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    true
                } else it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            } ?: run {
                online = false
            }
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            online = networkInfo != null && networkInfo.isConnected
        }

        EventPlatformClient.setEnabled(online)

        if (online) {
            WikipediaApp.instance.bus.post(NetworkConnectEvent())
        }
    }

    companion object {
        private val ONLINE_CHECK_THRESHOLD_MILLIS = TimeUnit.MINUTES.toMillis(1)
    }
}
