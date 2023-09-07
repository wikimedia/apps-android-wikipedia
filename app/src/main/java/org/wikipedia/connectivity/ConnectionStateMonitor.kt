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
    private var networkCallbackRegistered = false

    fun enable() {
        ensureNetworkCallbackRegistered()
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
        ensureNetworkCallbackRegistered()
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

    private fun ensureNetworkCallbackRegistered() {
        if (networkCallbackRegistered) {
            return
        }
        try {
            val connectivityManager = WikipediaApp.instance.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(this)
            } else {
                connectivityManager.registerNetworkCallback(
                    NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .build(), this
                )
            }
            networkCallbackRegistered = true
        } catch (e: Exception) {
            // Framework bug, will only be fixed in Android S:
            // https://issuetracker.google.com/issues/175055271
        }
    }

    private fun updateOnlineState() {
        val connectivityManager = WikipediaApp.instance.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        online = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            } else {
                connectivityManager.activeNetworkInfo?.isConnected == true
            }
        } catch (e: Exception) {
            // Framework bug, will only be fixed in Android S:
            // https://issuetracker.google.com/issues/175055271
            // Assume we're online, until the next call to update the state, which will happen shortly.
            true
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
