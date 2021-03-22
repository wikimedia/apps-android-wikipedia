package org.wikipedia.readinglist.sync

import android.app.Service
import android.content.AbstractThreadedSyncAdapter
import android.content.Intent
import android.os.IBinder

class ReadingListSyncService : Service() {
    override fun onCreate() {
        super.onCreate()
        synchronized(SYNC_ADAPTER_LOCK) {
            if (SYNC_ADAPTER == null) {
                SYNC_ADAPTER = ReadingListSyncAdapter(applicationContext, true)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return if (SYNC_ADAPTER == null) null else SYNC_ADAPTER?.syncAdapterBinder
    }

    companion object {
        private val SYNC_ADAPTER_LOCK = Any()
        private var SYNC_ADAPTER: AbstractThreadedSyncAdapter? = null
    }
}
