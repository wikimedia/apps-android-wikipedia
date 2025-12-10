package org.wikipedia.analytics.testkitchen

import org.wikimedia.testkitchen.LogAdapter
import org.wikipedia.util.log.L

class LogAdapterImpl : LogAdapter {
    override fun info(message: String, vararg args: Any) {
        L.i(message)
    }

    override fun warn(message: String, vararg args: Any) {
        L.w(message)
    }

    override fun error(message: String, vararg args: Any) {
        L.e(message)
    }
}
