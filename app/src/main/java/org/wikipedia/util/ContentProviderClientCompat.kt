package org.wikipedia.util

import android.content.ContentProviderClient
import android.os.Build

object ContentProviderClientCompat {
    @JvmStatic
    fun close(client: ContentProviderClient) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            client.close()
        } else {
            client.release()
        }
    }
}
