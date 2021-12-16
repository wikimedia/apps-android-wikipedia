package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.Serializable

@Serializable
open class TimedEvent(private val streamName: String) : Event(streamName) {

    companion object {
        private var startTime = 0L
        private var pauseTime = 0L
        val duration = System.currentTimeMillis() - startTime

        fun start() {
            pauseTime = System.currentTimeMillis()
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
}
