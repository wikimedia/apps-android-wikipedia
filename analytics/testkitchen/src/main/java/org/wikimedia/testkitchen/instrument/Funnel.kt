package org.wikimedia.testkitchen.instrument

import org.wikimedia.testkitchen.SessionController

class Funnel(
    val name: String? = null
) {
    val token = SessionController.generateSessionId()
    val sequence get() = _sequence
    private var startTime = System.currentTimeMillis()
    private var pauseTime = 0L
    private var _sequence: Int = 1

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
}
