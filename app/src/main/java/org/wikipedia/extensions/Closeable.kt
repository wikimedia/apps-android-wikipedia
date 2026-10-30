package org.wikipedia.extensions

import org.wikipedia.util.log.L
import java.io.Closeable

fun Closeable.closeSilently() {
    return try {
        this.close()
    } catch (e: Exception) {
        L.e(e)
    }
}
