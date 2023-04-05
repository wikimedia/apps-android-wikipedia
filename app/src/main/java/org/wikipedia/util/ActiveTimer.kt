package org.wikipedia.util

import java.util.concurrent.TimeUnit

class ActiveTimer {
    private var startMillis: Long = 0
    private var pauseMillis: Long = 0
    private var isPaused = false

    init {
        reset()
    }

    fun reset() {
        startMillis = System.currentTimeMillis()
        pauseMillis = startMillis
    }

    fun pause() {
        pauseMillis = System.currentTimeMillis()
        isPaused = true
    }

    fun resume() {
        startMillis += System.currentTimeMillis() - pauseMillis
        isPaused = false
    }

    val elapsedMillis: Long
        get() = if (isPaused) pauseMillis - startMillis else System.currentTimeMillis() - startMillis

    val elapsedSec: Int
        get() = TimeUnit.MILLISECONDS.toSeconds(elapsedMillis).toInt()
}
