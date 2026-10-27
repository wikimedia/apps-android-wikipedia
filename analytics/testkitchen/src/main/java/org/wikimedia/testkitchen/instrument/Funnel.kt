package org.wikimedia.testkitchen.instrument

import org.wikimedia.testkitchen.SessionController
import java.io.Serializable
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

class Funnel(
    val name: String? = null
) : Serializable {
    val token = SessionController.generateSessionId()
    val sequence get() = _sequence
    private var startTime = System.currentTimeMillis()
    private var pauseTime = 0L
    private var _sequence: Int = 1

    init {
        register(this)
    }

    fun touch() {
        _sequence++
    }

    val duration get() = (System.currentTimeMillis() - startTime).toInt()

    fun pause() {
        pauseTime = System.currentTimeMillis()
    }

    fun resume() {
        if (pauseTime > 0) {
            startTime += System.currentTimeMillis() - pauseTime
        }
        pauseTime = 0
    }

    fun reset() {
        startTime = System.currentTimeMillis()
    }

    fun addActionContext(actionContext: MutableMap<String, String>) {
        actionContext["time_spent_ms"] = duration.toString()
    }

    companion object {
        private val liveFunnels = ConcurrentHashMap<String, WeakReference<Funnel>>()

        private fun register(funnel: Funnel) {
            liveFunnels.values.removeAll { it.get() == null }
            liveFunnels[funnel.token] = WeakReference(funnel)
        }

        /**
         * A funnel passed through an Intent or Bundle arrives as a deserialized copy. Resolve it to the
         * in-memory funnel with the same token (if still alive), so both sides share one event sequence.
         */
        fun resolve(funnel: Funnel): Funnel {
            return liveFunnels[funnel.token]?.get() ?: funnel.also { register(it) }
        }
    }
}
