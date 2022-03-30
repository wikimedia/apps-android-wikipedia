package org.wikipedia.analytics.eventplatform

abstract class TimedEvent {
    private var startTime = 0L
    private var pauseTime = 0L

    val duration get() = (System.currentTimeMillis() - startTime).toInt()

    fun start() {
        startTime = System.currentTimeMillis()
    }

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
}
