package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed class TimedEvent(@Transient private val streamNameStr: String = "") : MobileAppsEvent(streamNameStr) {

    @Transient
    private var startTime = 0L

    @Transient
    private var pauseTime = 0L

    @Transient
    val duration = System.currentTimeMillis() - startTime

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

    fun resetDuration() {
        startTime = System.currentTimeMillis()
    }
}
