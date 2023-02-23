package org.wikipedia.connectivity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NetworkConnectivityReceiver : BroadcastReceiver() {

    private var connectionStateMonitor: ConnectionStateMonitor? = null

    override fun onReceive(context: Context, intent: Intent) {
        connectionStateMonitor = ConnectionStateMonitor()
        connectionStateMonitor?.enable(context)
    }

    fun isOnline(): Boolean {
        val isOnline = connectionStateMonitor?.isOnline() ?: true
        return isOnline
    }
}
