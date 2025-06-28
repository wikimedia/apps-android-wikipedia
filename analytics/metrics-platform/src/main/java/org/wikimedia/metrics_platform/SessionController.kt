package org.wikimedia.metrics_platform

import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Locale
import java.util.Random

/**
 * Manages sessions and session IDs for the Metrics Platform Client.
 *
 * A session begins when the application is launched and expires when the app is in the background
 * for 30 minutes or more.
 */
class SessionController internal constructor(
    private var sessionTouched: Instant? = Instant.now()
) {
    @get:Synchronized
    var sessionId = generateSessionId()
        private set

    @Synchronized
    fun touchSession() {
        if (sessionExpired()) {
            sessionId = generateSessionId()
        }
        sessionTouched = Instant.now()
    }

    @Synchronized
    fun beginSession() {
        sessionId = generateSessionId()
        sessionTouched = Instant.now()
    }

    @Synchronized
    fun closeSession() {
        // @ToDo Determine how to close the session.
        sessionTouched = Instant.now()
    }

    @Synchronized
    fun sessionExpired(): Boolean {
        return Duration.between(sessionTouched, Instant.now()) >= SESSION_LENGTH
    }

    companion object {
        private val SESSION_LENGTH = Duration.ofMinutes(30)
        private val RANDOM = SecureRandom()

        private fun generateSessionId(): String {
            val random: Random = RANDOM
            return String.format(Locale.US, "%08x", random.nextInt()) +
                    String.format(Locale.US, "%08x", random.nextInt()) +
                    String.format(Locale.US, "%04x", random.nextInt() and 0xFFFF)
        }
    }
}
