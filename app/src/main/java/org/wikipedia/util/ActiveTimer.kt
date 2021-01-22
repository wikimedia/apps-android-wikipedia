package org.wikipedia.util

import java.util.concurrent.TimeUnit

class ActiveTimer {
    private var startMillis: Long = 0
    private var pauseMillis: Long = 0

    init {
        reset()
    }

    fun reset() {
        startMillis = System.currentTimeMillis()
        pauseMillis = startMillis
    }

    fun pause() {
        pauseMillis = System.currentTimeMillis()
    }

    fun resume() {
        startMillis -= System.currentTimeMillis() - pauseMillis
    }

    val elapsedSec: Int
        get() = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startMillis).toInt()
}
