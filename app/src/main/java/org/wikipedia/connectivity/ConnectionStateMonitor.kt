package org.wikipedia.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.EventPlatformClient
import org.wikipedia.savedpages.SavedPageSyncService
import java.util.concurrent.TimeUnit

class ConnectionStateMonitor : ConnectivityManager.NetworkCallback() {

    interface Callback {
        fun onGoOnline()
        fun onGoOffline()
    }

    private var online = true
    private var prevOnline = true
    private var lastCheckedMillis = 0L
    private val callbacks = mutableListOf<Callback>()

    fun enable(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(this)
        } else {
            connectivityManager.registerNetworkCallback(NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build(), this)
        }
        updateOnlineState()
    }

    fun isOnline(): Boolean {
        if (System.currentTimeMillis() - lastCheckedMillis > ONLINE_CHECK_THRESHOLD_MILLIS) {
            updateOnlineState()
            lastCheckedMillis = System.currentTimeMillis()
        }
        return online
    }

    fun registerCallback(callback: Callback) {
        callbacks.add(callback)
    }

    fun unregisterCallback(callback: Callback) {
        callbacks.remove(callback)
    }

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        updateOnlineState()
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        updateOnlineState()
    }

    private fun updateOnlineState() {
        val connectivityManager = WikipediaApp.instance.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        online = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } else {
            connectivityManager.activeNetworkInfo?.isConnected == true
        }

        EventPlatformClient.setEnabled(online)

        if (online != prevOnline) {
            if (online) {
                WikipediaApp.instance.mainThreadHandler.post {
                    callbacks.forEach { it.onGoOnline() }
                }
                SavedPageSyncService.enqueue()
            } else {
                WikipediaApp.instance.mainThreadHandler.post {
                    callbacks.forEach { it.onGoOffline() }
                }
            }
            prevOnline = online
        }
    }

    companion object {
        private val ONLINE_CHECK_THRESHOLD_MILLIS = TimeUnit.MINUTES.toMillis(1)
    }
}
